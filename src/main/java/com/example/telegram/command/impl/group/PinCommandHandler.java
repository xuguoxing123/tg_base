package com.example.telegram.command.impl.group;

import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;
import org.springframework.stereotype.Component;

/**
 * /pin [message_id] — 置顶消息。
 * 不指定 message_id 时默认置顶触发本命令的那条消息。
 */
@Component
public class PinCommandHandler extends AbstractGroupAdminHandler {

    public PinCommandHandler(TelegramApiService apiService) {
        super(apiService);
    }

    @Override
    public String name() {
        return GroupAdminCommand.PIN.command();
    }

    @Override
    protected boolean doHandle(CommandContext context, String args) {
        int messageId = context.messageId();
        if (!args.isEmpty()) {
            try {
                messageId = Integer.parseInt(tokens(args)[0]);
            } catch (NumberFormatException e) {
                reply(context, "用法: /pin [消息ID]，消息ID 必须为数字");
                return false;
            }
        }
        boolean ok = apiService.pinChatMessage(context.chatId(), messageId);
        reply(context, ok ? "📌 已置顶消息 " + messageId : "❌ 置顶失败（可能 Bot 无置顶权限）");
        return ok;
    }
}
