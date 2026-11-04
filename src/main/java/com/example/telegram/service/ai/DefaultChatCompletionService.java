package com.example.telegram.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI 对话补全的默认实现：返回 application.properties 中配置的静态文本。
 * <p>
 * 后续接入真实 LLM（如通义千问 / OpenAI）时，只需新建一个类实现
 * {@link ChatCompletionService} 并标注 {@code @Primary}，本类会自动退居二线。
 *
 * <pre>
 * telegram.ai.fallback.enabled=true
 * telegram.ai.fallback.reply-text=抱歉，我暂时无法理解你的意思。输入 /help 查看可用命令。
 * telegram.ai.fallback.parse-mode=
 * </pre>
 */
@Service
public class DefaultChatCompletionService implements ChatCompletionService {

    private static final Logger log = LoggerFactory.getLogger(DefaultChatCompletionService.class);

    private final boolean enabled;
    private final String replyText;

    public DefaultChatCompletionService(
            @Value("${telegram.ai.fallback.enabled:true}") boolean enabled,
            @Value("${telegram.ai.fallback.reply-text:}") String replyText) {
        this.enabled = enabled;
        this.replyText = replyText != null && !replyText.isBlank()
                ? replyText
                : "抱歉，我暂时无法理解你的意思。输入 /help 查看可用命令。";
        log.info("DefaultChatCompletionService 初始化: enabled={}, replyText='{}'", this.enabled, truncate(this.replyText));
    }

    @Override
    public String complete(long userId, long chatId, String userText) {
        if (!enabled) return null;
        // TODO: 后续替换为真实 LLM API 调用，可基于 userId 维护多轮会话上下文
        log.debug("AI 兜底回复: userId={}, text='{}' → 返回默认文本", userId, truncate(userText));
        return replyText;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }
}
