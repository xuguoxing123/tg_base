package com.example.telegram.command.impl.group;

import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;
import org.springframework.stereotype.Component;

/**
 * /setdesc <文本> — 修改群描述。
 */
@Component
public class SetDescCommandHandler extends AbstractGroupAdminHandler {

    public SetDescCommandHandler(TelegramApiService apiService) {
        super(apiService);
    }

    @Override
    public String name() {
        return GroupAdminCommand.SET_DESC.command();
    }

    @Override
    protected boolean doHandle(CommandContext context, String args) {
        if (args.isEmpty()) {
            reply(context, "用法: /setdesc <新的群描述>");
            return false;
        }
        boolean ok = apiService.setChatDescription(context.chatId(), args);
        reply(context, ok ? "✅ 已更新群描述" : "❌ 修改群描述失败（可能 Bot 无改资料权限）");
        return ok;
    }
}
