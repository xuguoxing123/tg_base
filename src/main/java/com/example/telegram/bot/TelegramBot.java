package com.example.telegram.bot;

import com.example.telegram.callback.CallbackQueryHandler;
import com.example.telegram.command.BotCommandHandler;
import com.example.telegram.command.CommandContext;
import com.example.telegram.config.TelegramBotProperties;
import com.example.telegram.service.TelegramApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 机器人入口：只负责 Update 的解析与分发，不含任何业务逻辑。
 * 业务分别由 BotCommandHandler / CallbackQueryHandler 实现类承担，
 * 新增命令或按钮只需新增一个 Spring Bean，无需改动本类。
 */
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    private static final String CALLBACK_ANSWER_TEXT = "操作成功";

    private final TelegramBotProperties properties;
    private final TelegramApiService apiService;
    private final Map<String, BotCommandHandler> commandHandlers = new HashMap<>();
    private final Map<String, CallbackQueryHandler> callbackHandlers = new HashMap<>();
    private final BotCommandHandler fallbackCommandHandler;

    public TelegramBot(DefaultBotOptions botOptions,
                       TelegramBotProperties properties,
                       TelegramApiService apiService,
                       List<BotCommandHandler> handlers,
                       List<CallbackQueryHandler> callbackHandlerList) {
        super(botOptions);
        this.properties = properties;
        this.apiService = apiService;

        BotCommandHandler fallback = null;
        for (BotCommandHandler handler : handlers) {
            if (handler.name().isEmpty()) {
                fallback = handler;
            } else {
                commandHandlers.put(handler.name(), handler);
            }
        }
        this.fallbackCommandHandler = fallback;

        for (CallbackQueryHandler handler : callbackHandlerList) {
            callbackHandlers.put(handler.data(), handler);
        }

        if (properties.token() == null || properties.token().isBlank()) {
            throw new IllegalStateException(
                    "telegram.bot.token 未配置，请设置环境变量 TELEGRAM_BOT_TOKEN（在 @BotFather 获取）");
        }
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
        // 长轮询框架不允许异常外抛，统一在此兜底记录
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                dispatchMessage(update);
            }
            if (update.hasCallbackQuery()) {
                dispatchCallback(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("处理 Update 失败: {}", update, e);
        }
    }

    private void dispatchMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        BotCommandHandler handler = null;
        if (text.startsWith("/")) {
            // 去掉前导斜杠，并处理 "/start@MyBot" 提及形式与命令后参数
            String command = text.substring(1).split("[@\\s]", 2)[0];
            handler = commandHandlers.get(command);
        }
        if (handler == null) {
            handler = fallbackCommandHandler;
        }
        if (handler != null) {
            handler.handle(new CommandContext(chatId, text));
        }
    }

    private void dispatchCallback(CallbackQuery callbackQuery) {
        CallbackQueryHandler handler = callbackHandlers.get(callbackQuery.getData());
        if (handler != null) {
            handler.handle(callbackQuery);
        } else {
            log.warn("未找到回调处理器, callbackData: {}", callbackQuery.getData());
        }
        // 必须应答回调，否则按钮会一直处于加载状态
        apiService.answerCallback(callbackQuery.getId(), CALLBACK_ANSWER_TEXT);
    }
}
