package com.example.telegram.command;

/**
 * 动态命令定义（从 DB 加载后转为该 record，供 DynamicCommandHandler 使用）。
 */
public record CommandDefinition(
        String name,
        String replyType,     // "text" / "photo" / "buttons"
        String replyContent,  // 文本内容或图片 URL
        String replyMarkup    // JSON 按钮配置，仅 buttons 类型有值
) {
}
