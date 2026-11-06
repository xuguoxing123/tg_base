package com.example.telegram.command.impl.group;

import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;
import org.springframework.stereotype.Component;

/**
 * /settitle <文本> — 修改群名称。
 */
@Component
public class SetTitleCommandHandler extends AbstractGroupAdminHandler {

    public SetTitleCommandHandler(TelegramApiService apiService) {
        super(apiService);
    }

    @Override
    public String name() {
        return GroupAdminCommand.SET_TITLE.command();
    }

    @Override
    protected boolean doHandle(CommandContext context, String args) {
        if (args.isEmpty()) {
            reply(context, "用法: /settitle <新的群名称>");
            return false;
        }
        boolean ok = apiService.setChatTitle(context.chatId(), args);
        reply(context, ok ? "✅ 已更新群名称" : "❌ 修改群名称失败（可能 Bot 无改资料权限）");
        return ok;
    }
}
