package com.example.telegram.service;

import com.example.telegram.config.TelegramBotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Telegram API 统一出口：封装 execute 调用、异常处理与日志，
 * 业务代码不直接触碰 TelegramBots SDK。
 */
@Service
public class TelegramApiService extends DefaultAbsSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramApiService.class);

    public TelegramApiService(DefaultBotOptions botOptions, TelegramBotProperties properties) {
        super(botOptions, properties.token());
    }

    public void sendText(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        executeQuietly("sendMessage", () -> execute(message));
    }

    public void sendText(long chatId, String text, InlineKeyboardMarkup replyMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(replyMarkup);
        executeQuietly("sendMessage", () -> execute(message));
    }

    public void sendPhoto(long chatId, String photoUrl, String caption) {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(chatId);
        // 可以是网络图片URL，也可以是本地文件
        photo.setPhoto(new InputFile(photoUrl));
        photo.setCaption(caption);
        executeQuietly("sendPhoto", () -> execute(photo));
    }

    public void editMessageText(long chatId, int messageId, String newText) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setText(newText);
        executeQuietly("editMessageText", () -> execute(message));
    }

    public void answerCallback(String callbackQueryId, String alertText) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        answer.setText(alertText);
        answer.setShowAlert(false);
        executeQuietly("answerCallbackQuery", () -> execute(answer));
    }

    /**
     * 统一执行并吞掉异常仅记录日志，避免单个 API 调用失败中断整个更新处理。
     * 用函数式包装兼容 BotApiMethod 与媒体方法两套类型体系。
     */
    private void executeQuietly(String apiName, SendAction action) {
        try {
            action.run();
        } catch (TelegramApiException e) {
            log.error("调用 Telegram API 失败: {}", apiName, e);
        }
    }

    @FunctionalInterface
    private interface SendAction {
        void run() throws TelegramApiException;
    }
}
