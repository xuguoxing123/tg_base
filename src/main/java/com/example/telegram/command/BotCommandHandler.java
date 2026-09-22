package com.example.telegram.command;

/**
 * 文本命令处理器。实现类注册为 Spring Bean 后由 TelegramBot 自动收集分发。
 */
public interface BotCommandHandler {

    /**
     * 命令名，不带前导斜杠（如 "start"）；返回空串表示兜底处理器（处理非命令文本）。
     */
    String name();

    /**
     * @return true 表示处理成功（所有 Telegram API 调用均成功）；false 表示存在失败的调用
     */
    boolean handle(CommandContext context);
}
