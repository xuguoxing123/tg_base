package com.example.telegram.command;

/**
 * 命令上下文。
 *
 * @param chatId     会话 ID（群聊中为群的 ID，与用户 ID 不同）
 * @param userId     发送者的 Telegram 用户 ID，全局唯一且永不变，适合做白名单/限流等用户维度逻辑
 * @param arguments  用户发送的原始文本（含命令前缀）
 * @param role       用户角色："ADMIN" / "USER"，用于权限控制
 * @param blocked    用户是否在黑名单中，true = 已封禁
 * @param messageId  触发本命令的消息 ID（群管命令如 /pin 未显式指定时默认作用于该消息），非消息来源为 0
 */
public record CommandContext(long chatId, long userId, String arguments, String role, boolean blocked, int messageId) {

    /**
     * 向后兼容构造器（默认 USER + 未封禁 + messageId=0）。
     */
    public CommandContext(long chatId, long userId, String arguments) {
        this(chatId, userId, arguments, "USER", false, 0);
    }

    /**
     * 向后兼容构造器（role/blocked 显式指定，messageId=0）。
     */
    public CommandContext(long chatId, long userId, String arguments, String role, boolean blocked) {
        this(chatId, userId, arguments, role, blocked, 0);
    }
}
