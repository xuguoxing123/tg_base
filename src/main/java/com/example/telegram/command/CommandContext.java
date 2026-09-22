package com.example.telegram.command;

/**
 * 命令上下文。
 *
 * @param chatId    会话 ID（群聊中为群的 ID，与用户 ID 不同）
 * @param userId    发送者的 Telegram 用户 ID，全局唯一且永不变，适合做白名单/限流等用户维度逻辑
 * @param arguments 用户发送的原始文本（含命令前缀）
 */
public record CommandContext(long chatId, long userId, String arguments) {
}
