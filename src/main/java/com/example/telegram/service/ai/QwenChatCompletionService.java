package com.example.telegram.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于通义千问 (DashScope) 的 LLM 对话补全实现。
 * <p>
 * 标注 {@code @Primary}，Spring 会优先注入本类替代 {@link DefaultChatCompletionService}。
 * 当 API Key 未配置或 LLM 调用失败时，自动降级到默认静态文本回复。
 */
@Primary
@Service
public class QwenChatCompletionService implements ChatCompletionService {

    private static final Logger log = LoggerFactory.getLogger(QwenChatCompletionService.class);

    private final ChatClient chatClient;
    private final ChatMemoryManager memoryManager;
    private final DefaultChatCompletionService fallbackService;
    private final boolean available;

    public QwenChatCompletionService(
            ChatClient.Builder chatClientBuilder,
            ChatMemoryManager memoryManager,
            DefaultChatCompletionService fallbackService,
            @Value("${spring.ai.dashscope.api-key:}") String apiKey,
            @Value("${telegram.ai.fallback.enabled:true}") boolean fallbackEnabled) {

        this.memoryManager = memoryManager;
        this.fallbackService = fallbackService;

        // API Key 为空时不启用 LLM（避免启动报错，所有请求走降级）
        this.available = apiKey != null && !apiKey.isBlank();

        if (available) {
            this.chatClient = chatClientBuilder.build();
            log.info("QwenChatCompletionService 初始化成功: DashScope API Key 已配置");
        } else {
            this.chatClient = null;
            log.warn("QwenChatCompletionService: DASHSCOPE_API_KEY 未配置，将使用静态文本降级回复");
        }
    }

    @Override
    public String complete(long userId, long chatId, String userText) {
        if (!available) {
            log.debug("LLM 不可用，降级到静态文本: userId={}", userId);
            return fallbackService.complete(userId, chatId, userText);
        }

        try {
            // 构建带历史上下文的消息列表
            List<Message> messages = buildMessages(userId, userText);

            // 调用 LLM
            String reply = chatClient.prompt()
                    .messages(messages)
                    .user(userText)
                    .call()
                    .content();

            if (reply == null || reply.isBlank()) {
                log.warn("LLM 返回空内容，降级: userId={}", userId);
                return fallbackService.complete(userId, chatId, userText);
            }

            // 记录本轮对话到记忆
            memoryManager.addRound(userId, userText, reply);

            log.info("LLM 回复成功: userId={}, historyRounds={}, reply='{}'",
                    userId, memoryManager.getHistory(userId).length / 2, truncate(reply));
            return reply;

        } catch (Exception e) {
            log.error("LLM 调用异常，降级到静态文本: userId={}, error={}", userId, e.getMessage());
            return fallbackService.complete(userId, chatId, userText);
        }
    }

    @Override
    public boolean isEnabled() {
        // 只要兜底开关开启或 LLM 可用就视为 enabled
        return available || fallbackService.isEnabled();
    }

    /**
     * 将历史消息转换为 Spring AI Message 列表（不含最后一条 user 消息，因为它通过 .user() 单独传入）。
     */
    private List<Message> buildMessages(long userId, String currentUserText) {
        ChatMemoryManager.Message[] history = memoryManager.getHistory(userId);
        List<Message> messages = new ArrayList<>();

        for (ChatMemoryManager.Message msg : history) {
            switch (msg.role()) {
                case USER -> messages.add(new UserMessage(msg.content()));
                case ASSISTANT -> messages.add(new AssistantMessage(msg.content()));
            }
        }
        return messages;
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 100 ? s.substring(0, 100) + "..." : s;
    }
}
