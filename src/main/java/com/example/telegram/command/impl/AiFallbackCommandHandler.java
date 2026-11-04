package com.example.telegram.command.impl;

import com.example.telegram.command.BotCommandHandler;
import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;
import com.example.telegram.service.ai.ChatCompletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 兜底处理器：当用户发送的消息无法被任何命令/关键词匹配时触发。
 * <p>
 * 委托 {@link ChatCompletionService} 生成回复，当前默认实现返回配置文本；
 * 后续接入 LLM 后无需修改本类。
 * <p>
 * name() 返回空串 → CommandRegistry 将其识别为 fallback 处理器。
 */
@Component
public class AiFallbackCommandHandler implements BotCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(AiFallbackCommandHandler.class);

    private final TelegramApiService apiService;
    private final ChatCompletionService chatCompletionService;
    private final String parseMode;

    public AiFallbackCommandHandler(TelegramApiService apiService,
                                     ChatCompletionService chatCompletionService,
                                     @Value("${telegram.ai.fallback.parse-mode:}") String parseMode) {
        this.apiService = apiService;
        this.chatCompletionService = chatCompletionService;
        this.parseMode = parseMode != null && !parseMode.isBlank() ? parseMode : null;
    }

    @Override
    public String name() {
        // 空串标识为兜底处理器
        return "";
    }

    @Override
    public boolean handle(CommandContext context) {
        if (!chatCompletionService.isEnabled()) {
            log.debug("AI 兜底已禁用，静默忽略: chatId={}", context.chatId());
            return true;
        }

        String reply = chatCompletionService.complete(
                context.userId(), context.chatId(), context.arguments());

        if (reply == null || reply.isBlank()) {
            log.debug("AI 兜底返回空内容，静默跳过: chatId={}", context.chatId());
            return true;
        }

        log.info("AI 兜底回复: chatId={}, userId={}, input='{}'",
                context.chatId(), context.userId(), truncate(context.arguments()));
        return apiService.sendText(context.chatId(), reply, parseMode).isPresent();
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 80 ? s.substring(0, 80) + "..." : s;
    }
}
