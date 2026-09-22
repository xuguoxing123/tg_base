package com.example.telegram.command.impl;

import com.example.telegram.command.BotCommandHandler;
import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;
import org.springframework.stereotype.Component;

@Component
public class HelpCommandHandler implements BotCommandHandler {

    private static final String TEXT = "📖 帮助文档\n"
            + "这是一个 Java 开发的 Telegram 机器人示例。\n"
            + "支持文本回声、命令响应、图片发送和内联按钮。";

    private final TelegramApiService apiService;

    public HelpCommandHandler(TelegramApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public void handle(CommandContext context) {
        apiService.sendText(context.chatId(), TEXT);
    }
}
