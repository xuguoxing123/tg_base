package com.example.telegram.callback;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

/**
 * 内联按钮回调处理器。实现类注册为 Spring Bean 后由 TelegramBot 按 callback data 自动分发。
 */
public interface CallbackQueryHandler {

    /**
     * 与之匹配的按钮 callbackData（如 "btn_yes"）。
     */
    String data();

    /**
     * @return true 表示处理成功（所有 Telegram API 调用均成功）；false 表示存在失败的调用
     */
    boolean handle(CallbackQuery callbackQuery);
}
