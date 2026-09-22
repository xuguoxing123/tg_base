package com.example.telegram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 机器人配置，前缀 telegram.bot。
 * token 通过环境变量 TELEGRAM_BOT_TOKEN 注入，不落盘避免泄露。
 */
@ConfigurationProperties(prefix = "telegram.bot")
public record TelegramBotProperties(
        String token,
        String username,
        String proxyHost,
        int proxyPort,
        int longPollingTimeout
) {
}
