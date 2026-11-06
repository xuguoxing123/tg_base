package com.example.telegram.command.impl.group;

import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;
import org.springframework.stereotype.Component;

/**
 * /unban <user_id> — 解封群成员。
 */
@Component
public class UnbanCommandHandler extends AbstractGroupAdminHandler {

    public UnbanCommandHandler(TelegramApiService apiService) {
        super(apiService);
    }

    @Override
    public String name() {
        return GroupAdminCommand.UNBAN.command();
    }

    @Override
    protected boolean doHandle(CommandContext context, String args) {
        String[] tk = tokens(args);
        if (tk.length < 1) {
            reply(context, "用法: /unban <用户ID>");
            return false;
        }
        long targetUserId;
        try {
            targetUserId = Long.parseLong(tk[0]);
        } catch (NumberFormatException e) {
            reply(context, "用户ID 必须为数字");
            return false;
        }
        boolean ok = apiService.unbanChatMember(context.chatId(), targetUserId);
        reply(context, ok ? "🔓 已解封用户 " + targetUserId : "❌ 解封失败（可能 Bot 无封禁权限）");
        return ok;
    }
}
