package com.example.telegram.command.impl;

import com.example.telegram.command.BotCommandHandler;
import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;
import org.springframework.stereotype.Component;

@Component
public class PhotoCommandHandler implements BotCommandHandler {

    private static final String PHOTO_URL = "https://picsum.photos/400/300";
    private static final String CAPTION = "🖼️ 这是一张随机图片";

    private final TelegramApiService apiService;

    public PhotoCommandHandler(TelegramApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public String name() {
        return "photo";
    }

    @Override
    public void handle(CommandContext context) {
        apiService.sendPhoto(context.chatId(), PHOTO_URL, CAPTION);
    }
}
