package com.example.telegram.command.impl.group;

import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;
import org.springframework.stereotype.Component;

/**
 * /unpin [message_id] — 取消置顶。
 * 不指定 message_id 时取消当前置顶的那条消息。
 */
@Component
public class UnpinCommandHandler extends AbstractGroupAdminHandler {

    public UnpinCommandHandler(TelegramApiService apiService) {
        super(apiService);
    }

    @Override
    public String name() {
        return GroupAdminCommand.UNPIN.command();
    }

    @Override
    protected boolean doHandle(CommandContext context, String args) {
        int messageId = 0;
        if (!args.isEmpty()) {
            try {
                messageId = Integer.parseInt(tokens(args)[0]);
            } catch (NumberFormatException e) {
                reply(context, "用法: /unpin [消息ID]，消息ID 必须为数字");
                return false;
            }
        }
        boolean ok = apiService.unpinChatMessage(context.chatId(), messageId);
        reply(context, ok ? "✅ 已取消置顶" : "❌ 取消置顶失败（可能 Bot 无置顶权限）");
        return ok;
    }
}
