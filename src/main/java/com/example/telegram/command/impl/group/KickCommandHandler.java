package com.example.telegram.command.impl.group;

import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;
import org.springframework.stereotype.Component;

/**
 * /kick <user_id> — 将成员移出群聊（禁用其全部发言权限）。
 */
@Component
public class KickCommandHandler extends AbstractGroupAdminHandler {

    public KickCommandHandler(TelegramApiService apiService) {
        super(apiService);
    }

    @Override
    public String name() {
        return GroupAdminCommand.KICK.command();
    }

    @Override
    protected boolean doHandle(CommandContext context, String args) {
        String[] tk = tokens(args);
        if (tk.length < 1) {
            reply(context, "用法: /kick <用户ID>");
            return false;
        }
        long targetUserId;
        try {
            targetUserId = Long.parseLong(tk[0]);
        } catch (NumberFormatException e) {
            reply(context, "用户ID 必须为数字");
            return false;
        }
        boolean ok = apiService.kickChatMember(context.chatId(), targetUserId);
        reply(context, ok ? "👢 已移出用户 " + targetUserId : "❌ 移出失败（可能 Bot 无相关权限）");
        return ok;
    }
}
