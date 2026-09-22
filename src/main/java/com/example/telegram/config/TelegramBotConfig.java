package com.example.telegram.config;

import com.example.telegram.bot.TelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {

    /**
     * TelegramBots 底层的 Apache HttpClient 不读取系统代理，
     * 国内网络需在此处显式配置 HTTP 代理（VPN 客户端的本地代理端口）。
     */
    @Bean
    public DefaultBotOptions defaultBotOptions(TelegramBotProperties properties) {
        DefaultBotOptions options = new DefaultBotOptions();
        String proxyHost = properties.proxyHost();
        int proxyPort = properties.proxyPort();
        if (proxyHost != null && !proxyHost.isBlank() && proxyPort > 0) {
            options.setProxyType(DefaultBotOptions.ProxyType.HTTP);
            options.setProxyHost(proxyHost);
            options.setProxyPort(proxyPort);
        }
        // 缩短长轮询挂起时间：默认 50s 的 HTTPS 隧道容易被本地代理中途断开，
        // 触发 "SSL peer shut down incorrectly"。取值越小，暴露窗口越短（需 > 0）。
        int longPollingTimeout = properties.longPollingTimeout();
        if (longPollingTimeout > 0) {
            options.setGetUpdatesTimeout(longPollingTimeout);
        }
        return options;
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBot bot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
        return api;
    }
}
