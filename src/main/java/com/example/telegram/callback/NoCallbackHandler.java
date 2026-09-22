package com.example.telegram.callback;

import com.example.telegram.service.TelegramApiService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
public class NoCallbackHandler implements CallbackQueryHandler {

    private final TelegramApiService apiService;

    public NoCallbackHandler(TelegramApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public String data() {
        return "btn_no";
    }

    @Override
    public boolean handle(CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();
        return apiService.editMessageText(message.getChatId(), message.getMessageId(), "❌ 你选择了【否】").isPresent();
    }
}
