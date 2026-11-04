package com.example.telegram.command;

/**
 * 入群欢迎规则：当检测到新成员加入群聊时自动发送欢迎语。
 *
 * @param commandName 来源命令名（用于日志追踪）
 * @param welcomeText 欢迎语文本
 * @param parseMode   格式化模式（可为 null）
 */
public record WelcomeRule(
        String commandName,
        String welcomeText,
        String parseMode
) {
}
