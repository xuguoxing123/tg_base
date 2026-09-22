package com.example.telegram.command;

/**
 * 命令上下文。
 *
 * @param chatId    会话 ID
 * @param arguments 用户发送的原始文本（含命令前缀）
 */
public record CommandContext(long chatId, String arguments) {
}
