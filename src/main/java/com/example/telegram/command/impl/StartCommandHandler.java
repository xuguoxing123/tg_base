package com.example.telegram.command.impl;

import com.example.telegram.command.BotCommandHandler;
import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;
import org.springframework.stereotype.Component;

@Component
public class StartCommandHandler implements BotCommandHandler {

    private static final String TEXT = "👋 欢迎使用我的 Telegram 机器人！\n\n"
            + "可用命令：\n"
            + "/help  - 查看帮助\n"
            + "/photo - 发送一张图片\n"
            + "/buttons - 发送按钮";

    private final TelegramApiService apiService;

    public StartCommandHandler(TelegramApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public String name() {
        return "start";
    }

    @Override
    public void handle(CommandContext context) {
        apiService.sendText(context.chatId(), TEXT);
    }
}
