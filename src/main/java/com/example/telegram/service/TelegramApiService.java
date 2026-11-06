package com.example.telegram.service;

import com.example.telegram.config.TelegramBotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatDescription;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatTitle;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
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
        return sendText(chatId, text, (String) null);
    }

    /**
     * 发送文本消息，支持可选的 Markdown/HTML 格式化。
     *
     * @param parseMode null/空 = 纯文本；"MarkdownV2" 或 "HTML" = 格式化渲染
     */
    public Optional<Message> sendText(long chatId, String text, String parseMode) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        if (parseMode != null && !parseMode.isBlank()) {
            message.setParseMode(parseMode);
        }
        return executeQuietly("sendMessage", () -> execute(message));
    }

    public Optional<Message> sendText(long chatId, String text, InlineKeyboardMarkup replyMarkup) {
        return sendText(chatId, text, null, replyMarkup);
    }

    public Optional<Message> sendText(long chatId, String text, String parseMode, InlineKeyboardMarkup replyMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        if (parseMode != null && !parseMode.isBlank()) {
            message.setParseMode(parseMode);
        }
        message.setReplyMarkup(replyMarkup);
        return executeQuietly("sendMessage", () -> execute(message));
    }

    /**
     * 发送带自定义回复键盘（ReplyKeyboardMarkup）的文本消息。
     * 用户点击键盘按钮后，按钮文字会作为普通消息发送给机器人。
     */
    public Optional<Message> sendTextWithKeyboard(long chatId, String text, String parseMode, ReplyKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        if (parseMode != null && !parseMode.isBlank()) {
            message.setParseMode(parseMode);
        }
        message.setReplyMarkup(keyboard);
        return executeQuietly("sendReplyKeyboard", () -> execute(message));
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

    // ===== 群组管理 API（返回 Boolean 结果，失败/异常统一映射为 false）=====

    /** 置顶消息 */
    public boolean pinChatMessage(long chatId, int messageId) {
        PinChatMessage m = new PinChatMessage();
        m.setChatId(chatId);
        m.setMessageId(messageId);
        return executeBool("pinChatMessage", () -> execute(m));
    }

    /** 取消置顶（messageId<=0 表示取消当前置顶的那条） */
    public boolean unpinChatMessage(long chatId, int messageId) {
        UnpinChatMessage m = new UnpinChatMessage();
        m.setChatId(chatId);
        if (messageId > 0) m.setMessageId(messageId);
        return executeBool("unpinChatMessage", () -> execute(m));
    }

    /** 封禁成员（untilDate 为 Unix 秒，null/0 表示永久） */
    public boolean banChatMember(long chatId, long userId, Integer untilDate) {
        BanChatMember m = new BanChatMember();
        m.setChatId(chatId);
        m.setUserId(userId);
        if (untilDate != null && untilDate > 0) m.setUntilDate(untilDate);
        m.setRevokeMessages(true);
        return executeBool("banChatMember", () -> execute(m));
    }

    /** 解封成员 */
    public boolean unbanChatMember(long chatId, long userId) {
        UnbanChatMember m = new UnbanChatMember();
        m.setChatId(chatId);
        m.setUserId(userId);
        return executeBool("unbanChatMember", () -> execute(m));
    }

    /** 移出群聊（Bot API 6.x 无独立 kick 接口，用 RestrictChatMember + 全禁用权限实现） */
    public boolean kickChatMember(long chatId, long userId) {
        RestrictChatMember m = new RestrictChatMember();
        m.setChatId(chatId);
        m.setUserId(userId);
        ChatPermissions perms = new ChatPermissions();
        perms.setCanSendMessages(false);
        perms.setCanSendAudios(false);
        perms.setCanSendDocuments(false);
        perms.setCanSendPhotos(false);
        perms.setCanSendVideos(false);
        perms.setCanSendVideoNotes(false);
        perms.setCanSendVoiceNotes(false);
        perms.setCanSendPolls(false);
        perms.setCanSendOtherMessages(false);
        perms.setCanAddWebPagePreviews(false);
        perms.setCanChangeInfo(false);
        perms.setCanInviteUsers(false);
        perms.setCanPinMessages(false);
        perms.setCanManageTopics(false);
        m.setPermissions(perms);
        return executeBool("kickChatMember", () -> execute(m));
    }

    /** 修改群描述 */
    public boolean setChatDescription(long chatId, String description) {
        SetChatDescription m = new SetChatDescription();
        m.setChatId(chatId);
        m.setDescription(description);
        return executeBool("setChatDescription", () -> execute(m));
    }

    /** 修改群名称 */
    public boolean setChatTitle(long chatId, String title) {
        SetChatTitle m = new SetChatTitle();
        m.setChatId(chatId);
        m.setTitle(title);
        return executeBool("setChatTitle", () -> execute(m));
    }

    /**
     * 查询某用户在群中的成员状态，用于群管命令的权限校验。
     *
     * @return status 字符串（creator/administrator/member/left/banned/restricted），查询失败返回 null
     */
    public String getChatMemberStatus(long chatId, long userId) {
        GetChatMember m = new GetChatMember();
        m.setChatId(chatId);
        m.setUserId(userId);
        try {
            ChatMember cm = execute(m);
            return cm != null ? cm.getStatus() : null;
        } catch (TelegramApiException e) {
            log.error("调用 Telegram API 失败: getChatMember", e);
            return null;
        }
    }

    /**
     * 执行返回 Boolean 的 API（群管类接口），异常吞掉仅记日志并返回 false。
     */
    private boolean executeBool(String apiName, BoolCall call) {
        try {
            Boolean result = call.call();
            boolean ok = Boolean.TRUE.equals(result);
            log.debug("Telegram API {}: {}", apiName, ok);
            return ok;
        } catch (TelegramApiException e) {
            log.error("调用 Telegram API 失败: {}", apiName, e);
            return false;
        }
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

    @FunctionalInterface
    private interface BoolCall {
        Boolean call() throws TelegramApiException;
    }
}
