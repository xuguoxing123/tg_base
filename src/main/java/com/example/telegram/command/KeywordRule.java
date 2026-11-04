package com.example.telegram.command;

/**
 * 关键词触发规则：当用户发送非命令文本且包含指定关键词时，自动回复内容。
 *
 * @param keyword   触发关键词（小写存储，匹配时忽略大小写）
 * @param replyType 回复类型（复用 DynamicCommandHandler 逻辑）
 * @param content   回复内容
 * @param markup    按钮/键盘 JSON 配置（可为 null）
 * @param parseMode 格式化模式（可为 null）
 */
public record KeywordRule(
        String keyword,
        String replyType,
        String content,
        String markup,
        String parseMode
) {
}
