package com.example.telegram.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

/**
 * 消息日志实体，记录每条经过命令分发的消息。
 */
@TableName("cs_bot_message_log")
public class MessageLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long chatId;
    private String commandName;   // 命令名或关键词，非命令为 null
    private String handlerType;   // 处理器类型简名
    private Integer success;      // 1=成功 0=失败

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public String getCommandName() { return commandName; }
    public void setCommandName(String commandName) { this.commandName = commandName; }

    public String getHandlerType() { return handlerType; }
    public void setHandlerType(String handlerType) { this.handlerType = handlerType; }

    public Integer getSuccess() { return success; }
    public void setSuccess(Integer success) { this.success = success; }

    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }
}
