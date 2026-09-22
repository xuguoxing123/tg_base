package com.example.telegram.command.impl;

import com.example.telegram.command.BotCommandHandler;
import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;
import org.springframework.stereotype.Component;

/**
 * 兜底处理器：非命令文本原样回声。
 */
@Component
public class EchoCommandHandler implements BotCommandHandler {

    private final TelegramApiService apiService;

    public EchoCommandHandler(TelegramApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public String name() {
        // 空串标识为默认兜底处理器
        return "";
    }

    @Override
    public boolean handle(CommandContext context) {
        return apiService.sendText(context.chatId(), "📩 你发送的消息是：\n" + context.arguments()).isPresent();
    }
}
