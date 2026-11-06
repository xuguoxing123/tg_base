package com.example.telegram.command;

/**
 * 动态命令定义（从 DB 加载后转为该 record，供 DynamicCommandHandler 使用）。
 *
 * @param parseMode    文本格式化模式：null/空 = 纯文本，"MarkdownV2" 或 "HTML"
 * @param systemPrompt AI 兜底时该命令对应的系统提示词（覆盖全局配置），可为 null
 */
public record CommandDefinition(
        String name,
        String replyType,     // "text" / "photo" / "buttons"
        String replyContent,  // 文本内容或图片 URL
        String replyMarkup,   // JSON 按钮配置，仅 buttons 类型有值
        String parseMode,     // "MarkdownV2" / "HTML" / null
        String systemPrompt   // AI 兜底系统提示词，可为 null
) {
}
