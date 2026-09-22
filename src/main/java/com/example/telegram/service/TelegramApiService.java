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
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

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

    public Optional<Message> sendText(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        return executeQuietly("sendMessage", () -> execute(message));
    }

    public Optional<Message> sendText(long chatId, String text, InlineKeyboardMarkup replyMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(replyMarkup);
        return executeQuietly("sendMessage", () -> execute(message));
    }

    public Optional<Message> sendPhoto(long chatId, String photoUrl, String caption) {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(chatId);
        // 可以是网络图片URL，也可以是本地文件
        photo.setPhoto(new InputFile(photoUrl));
        photo.setCaption(caption);
        return executeQuietly("sendPhoto", () -> execute(photo));
    }

    public Optional<Message> editMessageText(long chatId, int messageId, String newText) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setText(newText);
        // EditMessageText 响应声明为 Serializable（兼容 inline 消息场景），普通会话下实际就是 Message
        return executeQuietly("editMessageText", () -> (Message) execute(message));
    }

    public boolean answerCallback(String callbackQueryId, String alertText) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        answer.setText(alertText);
        answer.setShowAlert(false);
        // 该 API 的响应体就是 Boolean，失败时用 false 表达
        return executeQuietly("answerCallbackQuery", () -> execute(answer)).orElse(false);
    }

    /**
     * 统一执行并吞掉异常仅记录日志，避免单个 API 调用失败中断整个更新处理。
     * 成功时返回 Telegram 服务器确认的响应体（如实际发送出的 Message，含真实 messageId、时间戳）。
     *
     * @return 有值表示调用成功；empty 表示抛出 TelegramApiException（详情已记 ERROR 日志）
     */
    private <T> Optional<T> executeQuietly(String apiName, SendCall<T> call) {
        try {
            T result = call.call();
            log.debug("Telegram API 成功: {}, 响应: {}", apiName, result);
            return Optional.ofNullable(result);
        } catch (TelegramApiException e) {
            log.error("调用 Telegram API 失败: {}", apiName, e);
            return Optional.empty();
        }
    }

    @FunctionalInterface
    private interface SendCall<T> {
        T call() throws TelegramApiException;
    }
}
