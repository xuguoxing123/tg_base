package com.example.telegram.service.ai;

/**
 * AI 对话补全服务接口 —— 为 Bot 兜底回复提供智能对话能力。
 * <p>
 * 当前阶段使用 {@link DefaultChatCompletionService}（返回配置的静态文本），
 * 后续接入 LLM API 时只需新建实现类并标注 {@code @Primary}，无需改动 Handler。
 */
public interface ChatCompletionService {

    /**
     * 根据用户输入生成回复。
     *
     * @param userId   发送者的 Telegram 用户 ID（可用于会话上下文管理）
     * @param chatId   会话 ID（群聊/私聊区分）
     * @param userText 用户原始消息文本
     * @return 回复内容；返回 null 或空串表示无法应答，由调用方决定降级策略
     */
    String complete(long userId, long chatId, String userText);

    /**
     * 是否启用（可由配置开关控制）。
     */
    default boolean isEnabled() {
        return true;
    }
}
