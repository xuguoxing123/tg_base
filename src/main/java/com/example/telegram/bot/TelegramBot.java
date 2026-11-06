package com.example.telegram.bot;

import com.example.telegram.callback.CallbackQueryHandler;
import com.example.telegram.command.BotCommandHandler;
import com.example.telegram.command.CommandContext;
import com.example.telegram.command.CommandRegistry;
import com.example.telegram.command.KeywordRule;
import com.example.telegram.command.TemplateRenderer;
import com.example.telegram.command.WelcomeRule;
import com.example.telegram.config.TelegramBotProperties;
import com.example.telegram.service.TelegramApiService;
import com.example.telegram.service.TelegramUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 机器人入口：解析 Update 并按 chatId 哈希分片到工作线程池，
 * 命令查找委托给 CommandRegistry（支持内置 + DB 动态命令），
 * 回调处理器仍使用静态 Map（按钮逻辑一般不需要运营动态改）。
 */
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    private static final String CALLBACK_ANSWER_TEXT = "操作成功";

    private final TelegramBotProperties properties;
    private final TelegramApiService apiService;
    private final CommandRegistry registry;
    private final RateLimiter rateLimiter;
    private final TelegramUserService userService;
    private final Map<String, CallbackQueryHandler> callbackHandlers = new HashMap<>();

    /** 按 chatId 哈希分片的单线程工作池组，保证同一会话消息有序 */
    private final ExecutorService[] workers;

    public TelegramBot(DefaultBotOptions botOptions,
                       TelegramBotProperties properties,
                       TelegramApiService apiService,
                       CommandRegistry registry,
                       RateLimiter rateLimiter,
                       TelegramUserService userService,
                       List<CallbackQueryHandler> callbackHandlerList,
                       @Value("${telegram.worker-pool-size:4}") int poolSize,
                       @Value("${telegram.worker-queue-capacity:200}") int queueCapacity) {
        super(botOptions);
        this.properties = properties;
        this.apiService = apiService;
        this.registry = registry;
        this.rateLimiter = rateLimiter;
        this.userService = userService;

        for (CallbackQueryHandler handler : callbackHandlerList) {
            callbackHandlers.put(handler.data(), handler);
        }

        // 初始化工作线程池：有界队列 + AbortPolicy，防止刷屏时任务无限堆积导致 OOM
        this.workers = new ExecutorService[poolSize];
        for (int i = 0; i < poolSize; i++) {
            final int slot = i;
            workers[i] = new ThreadPoolExecutor(
                    1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(queueCapacity),
                    r -> {
                        Thread t = new Thread(r, "tg-worker-" + slot);
                        t.setDaemon(true);
                        return t;
                    },
                    new ThreadPoolExecutor.AbortPolicy());
        }

        if (properties.token() == null || properties.token().isBlank()) {
            throw new IllegalStateException(
                    "telegram.bot.token 未配置，请设置环境变量 TELEGRAM_BOT_TOKEN（在 @BotFather 获取）");
        }
        log.info("TelegramBot 初始化完成: bot={}, 命令注册表={} 条, 回调处理器={} 个, 工作线程={} 个",
                properties.username(), registry.size(), callbackHandlers.size(), poolSize);
    }

    @Override
    public String getBotToken() {
        return properties.token();
    }

    @Override
    public String getBotUsername() {
        return properties.username();
    }

    @Override
    public void onUpdateReceived(Update update) {
        long userId = extractUserId(update);
        // 1) 每用户限流：狂刷机器人的用户直接丢弃，保护服务器与 Telegram API 配额
        if (!rateLimiter.tryAcquire(userId)) {
            log.warn("用户 {} 触发限流，丢弃消息", userId);
            return;
        }

        // 2) 按 chatId 哈希分片路由（同一用户/群的消息始终进同一线程 → 有序）
        long chatId = extractChatId(update);
        int slot = (int) (Math.abs(chatId) % workers.length);
        try {
            workers[slot].submit(() -> processUpdate(update));
        } catch (RejectedExecutionException e) {
            // 3) 有界队列已满：说明该分片积压严重，主动丢弃新任务避免内存继续膨胀
            log.warn("工作线程池 slot={} 队列已满，丢弃来自 chatId={} 的消息", slot, chatId);
        }
    }

    private static long extractUserId(Update update) {
        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            return update.getMessage().getFrom().getId();
        }
        if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
            return update.getCallbackQuery().getFrom().getId();
        }
        return 0L;
    }

    private void processUpdate(Update update) {
        // 长轮询框架不允许异常外抛，统一在此兜底记录
        try {
            if (update.hasMessage()) {
                Message msg = update.getMessage();
                // 入群检测：有人加入群聊时发送欢迎语
                if (msg.getNewChatMembers() != null && !msg.getNewChatMembers().isEmpty()) {
                    dispatchWelcome(msg);
                }
                if (msg.hasText()) {
                    dispatchMessage(update);
                }
            }
            if (update.hasCallbackQuery()) {
                dispatchCallback(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("处理 Update 失败: {}", update, e);
        }
    }

    /**
     * 入群欢迎：当检测到新成员加入群聊时，自动发送配置的欢迎语。
     */
    private void dispatchWelcome(Message message) {
        long chatId = message.getChatId();
        String chatTitle = message.getChat() != null ? message.getChat().getTitle() : "未知群";
        User[] newMembers = message.getNewChatMembers().toArray(new User[0]);

        registry.getWelcomeRule().ifPresent(rule -> {
            StringBuilder sb = new StringBuilder();
            for (User u : newMembers) {
                String name = u.getFirstName();
                if (u.getUserName() != null) name += "(@" + u.getUserName() + ")";
                sb.append(name).append(", ");
            }
            if (sb.length() > 2) sb.setLength(sb.length() - 2);

            // 使用 TemplateRenderer 统一渲染（兼容旧语法 {user}/{chat} + 新语法 {{date}}/{{time}} 等）
            String welcomeMsg = TemplateRenderer.renderWelcome(rule.welcomeText(), sb.toString(), chatTitle);
            log.info("入群欢迎: chat={}, 新成员={}", chatId, sb);
            apiService.sendText(chatId, welcomeMsg, rule.parseMode());
        });
    }

    private void dispatchMessage(Update update) {
        Message msg = update.getMessage();
        long chatId = msg.getChatId();
        User from = msg.getFrom();
        long userId = from != null ? from.getId() : 0L;
        String text = msg.getText();

        // 自动 upsert 用户信息（记录活跃时间、累计消息数）
        if (from != null && userId > 0) {
            userService.recordInteraction(userId, from.getUserName(), from.getFirstName(), from.getLastName());
        }

        // 黑名单检查：封禁用户直接忽略
        if (userId > 0 && userService.isBlocked(userId)) {
            log.info("黑名单用户消息已忽略: userId={}, chatId={}", userId, chatId);
            return;
        }

        // 获取用户角色
        String role = userId > 0 ? userService.getRole(userId) : "USER";

        BotCommandHandler handler = null;
        boolean isCommand = text.startsWith("/");

        if (isCommand) {
            String command = text.substring(1).split("[@\\s]", 2)[0];
            handler = registry.lookup(command).orElse(null);
        }

        if (handler == null) {
            // 非命令文本或命令未匹配 → 尝试关键词自动回复
            if (!isCommand) {
                var kwRule = registry.lookupKeyword(text);
                if (kwRule.isPresent()) {
                    handleKeywordReply(chatId, userId, text, kwRule.get());
                    return;
                }
            }
            handler = registry.getFallback();
        }

        if (handler != null) {
            CommandContext context = new CommandContext(chatId, userId, text, role, false);
            String handlerName = handler.getClass().getSimpleName();
            long start = System.nanoTime();
            boolean success = false;
            log.info("开始执行处理器: {}, 参数: {}", handlerName, context);
            try {
                success = handler.handle(context);
            } finally {
                log.info("处理器执行结束: {}, 成功: {}, 耗时: {}ms", handlerName, success, elapsedMs(start));
            }
        }
    }

    /**
     * 关键词匹配命中后，根据规则的 replyType 分派发送逻辑。
     */
    private void handleKeywordReply(long chatId, long userId, String matchedText, KeywordRule rule) {
        log.info("关键词命中: keyword='{}', replyType='{}', text='{}'",
                rule.keyword(), rule.replyType(), matchedText);
        // 渲染模板变量
        CommandContext ctx = new CommandContext(chatId, userId, matchedText);
        String content = TemplateRenderer.render(rule.content(), ctx);
        switch (rule.replyType()) {
            case "photo" -> apiService.sendPhoto(chatId, content, content);
            default -> apiService.sendText(chatId, content, rule.parseMode());
        }
    }

    private void dispatchCallback(CallbackQuery callbackQuery) {
        log.info("收到按钮回调: userId={}, callbackData={}",
                callbackQuery.getFrom() != null ? callbackQuery.getFrom().getId() : 0L,
                callbackQuery.getData());
        CallbackQueryHandler handler = callbackHandlers.get(callbackQuery.getData());
        if (handler != null) {
            String handlerName = handler.getClass().getSimpleName();
            long start = System.nanoTime();
            boolean success = false;
            log.info("开始执行回调处理器: {}, chatId: {}, messageId: {}", handlerName,
                    callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());
            try {
                success = handler.handle(callbackQuery);
            } finally {
                log.info("回调处理器执行结束: {}, 成功: {}, 耗时: {}ms", handlerName, success, elapsedMs(start));
            }
        } else {
            log.warn("未找到回调处理器, callbackData: {}", callbackQuery.getData());
        }
        // 必须应答回调，否则按钮会一直处于加载状态
        apiService.answerCallback(callbackQuery.getId(), CALLBACK_ANSWER_TEXT);
    }

    private static long extractChatId(Update update) {
        if (update.hasMessage()) return update.getMessage().getChatId();
        if (update.hasCallbackQuery()) return update.getCallbackQuery().getMessage().getChatId();
        return 0L;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
