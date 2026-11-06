package com.example.telegram.command.impl.group;

import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;
import org.springframework.stereotype.Component;

/**
 * /ban <user_id> [天数] — 封禁群成员。
 * 可选第二个参数为封禁天数，缺省为永久。
 */
@Component
public class BanCommandHandler extends AbstractGroupAdminHandler {

    public BanCommandHandler(TelegramApiService apiService) {
        super(apiService);
    }

    @Override
    public String name() {
        return GroupAdminCommand.BAN.command();
    }

    @Override
    protected boolean doHandle(CommandContext context, String args) {
        String[] tk = tokens(args);
        if (tk.length < 1) {
            reply(context, "用法: /ban <用户ID> [天数]");
            return false;
        }
        long targetUserId;
        Integer untilDate = null;
        try {
            targetUserId = Long.parseLong(tk[0]);
            if (tk.length >= 2) {
                long days = Long.parseLong(tk[1]);
                untilDate = (int) (System.currentTimeMillis() / 1000 + days * 86400);
            }
        } catch (NumberFormatException e) {
            reply(context, "用户ID 和天数必须为数字");
            return false;
        }
        boolean ok = apiService.banChatMember(context.chatId(), targetUserId, untilDate);
        reply(context, ok ? "🔒 已封禁用户 " + targetUserId : "❌ 封禁失败（可能 Bot 无封禁权限）");
        return ok;
    }
}
