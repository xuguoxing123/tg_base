package com.example.telegram.command.impl.group;

/**
 * 群管理命令名枚举。
 * <p>
 * 收敛各 {@link AbstractGroupAdminHandler} 子类 {@code name()} 返回的命令标识，
 * 避免散落的字符串魔法值；命令名不带前导斜杠，与 {@code BotCommandHandler#name()} 约定一致。
 */
public enum GroupAdminCommand {

    /** 置顶消息：/pin [message_id]，缺省作用于触发命令的那条消息 */
    PIN("pin"),
    /** 取消置顶：/unpin [message_id]，缺省取消当前置顶的那条 */
    UNPIN("unpin"),
    /** 封禁成员：/ban <user_id> [天数]，可选封禁时长，缺省永久 */
    BAN("ban"),
    /** 解封成员：/unban <user_id> */
    UNBAN("unban"),
    /** 移出/禁言成员：/kick <user_id>（禁用其全部发言权限） */
    KICK("kick"),
    /** 修改群描述：/setdesc <text> */
    SET_DESC("setdesc"),
    /** 修改群名称：/settitle <text> */
    SET_TITLE("settitle");

    private final String command;

    GroupAdminCommand(String command) {
        this.command = command;
    }

    /** 命令标识（不带前导斜杠）。 */
    public String command() {
        return command;
    }
}
