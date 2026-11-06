package com.example.telegram.command.impl.group;

import com.example.telegram.command.BotCommandHandler;
import com.example.telegram.command.CommandContext;
import com.example.telegram.service.TelegramApiService;

/**
 * 群管理命令处理器基类。
 * <p>
 * 统一负责：参数拆分、群聊环境校验、操作者管理员权限校验，子类只需实现具体动作。
 * 这类命令涉及权限敏感操作，因此作为内置处理器（不走 DB 动态配置）。
 */
public abstract class AbstractGroupAdminHandler implements BotCommandHandler {

    protected final TelegramApiService apiService;

    protected AbstractGroupAdminHandler(TelegramApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public boolean handle(CommandContext context) {
        // 1) 去掉前导 "/cmd " 得到参数字符串
        String args = stripCommand(context.arguments());

        // 2) 必须在群聊/频道中执行
        if (context.chatId() > 0) {
            apiService.sendText(context.chatId(), "⚠️ " + name() + " 只能在群聊中使用。");
            return false;
        }

        // 3) 权限校验：操作者必须是群创建者或管理员
        if (!isOperatorAdmin(context)) {
            apiService.sendText(context.chatId(), "🚫 你没有权限执行此操作（需群管理员身份）。");
            return false;
        }

        // 4) 交由子类执行具体动作
        return doHandle(context, args);
    }

    /**
     * 子类实现的具体群管动作。
     *
     * @param context 命令上下文
     * @param args    去掉命令前缀后的参数字符串
     * @return 是否执行成功
     */
    protected abstract boolean doHandle(CommandContext context, String args);

    /** 去掉前导的 "/name"，返回其后的参数（已 trim）。 */
    protected String stripCommand(String text) {
        if (text == null) return "";
        int space = text.indexOf(' ');
        return space < 0 ? "" : text.substring(space + 1).trim();
    }

    /** 按空白拆分参数。 */
    protected String[] tokens(String args) {
        return args.isEmpty() ? new String[0] : args.split("\\s+");
    }

    /**
     * 判断操作者在群中是否为管理员/创建者。
     * 优先信任 Telegram 官方成员状态（GetChatMember），DB 角色仅作为无网络查询时的辅助。
     */
    protected boolean isOperatorAdmin(CommandContext context) {
        String status = apiService.getChatMemberStatus(context.chatId(), context.userId());
        if (status != null) {
            return "creator".equals(status) || "administrator".equals(status);
        }
        // Telegram 查询失败时降级到 DB 角色
        return "ADMIN".equalsIgnoreCase(context.role());
    }

    protected void reply(CommandContext context, String text) {
        apiService.sendText(context.chatId(), text);
    }
}
