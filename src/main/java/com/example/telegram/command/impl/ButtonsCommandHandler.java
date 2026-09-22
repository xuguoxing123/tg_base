package com.example.telegram.command.impl;

import com.example.telegram.command.BotCommandHandler;
import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class ButtonsCommandHandler implements BotCommandHandler {

    private final TelegramApiService apiService;

    public ButtonsCommandHandler(TelegramApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public String name() {
        return "buttons";
    }

    @Override
    public boolean handle(CommandContext context) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        InlineKeyboardButton btnYes = new InlineKeyboardButton();
        btnYes.setText("✅ 是");
        btnYes.setCallbackData("btn_yes");

        InlineKeyboardButton btnNo = new InlineKeyboardButton();
        btnNo.setText("❌ 否");
        btnNo.setCallbackData("btn_no");

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(btnYes);
        row.add(btnNo);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row);
        markup.setKeyboard(rows);

        return apiService.sendText(context.chatId(), "请选择一个选项：", markup).isPresent();
    }
}
