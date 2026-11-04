package com.example.telegram.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

/**
 * 广播任务配置实体。运营通过后台配置“目标群 + cron 表达式 + 回复内容”，
 * 由 BroadcastTaskRunner 按周期扫描并推送到指定 chat。
 */
@TableName("cs_bot_broadcast_task")
public class BroadcastTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;              // 任务唯一标识
    private String description;       // 任务说明
    private Long chatId;              // 目标会话/群 ID
    private String cronExpression;    // Spring cron 表达式（6 位）
    private String replyType;         // text/photo/buttons/keyboard
    private String replyContent;      // 文本内容或图片 URL
    private String replyMarkup;       // 按钮/键盘 JSON 配置
    private String parseMode;         // MarkdownV2/HTML/null
    private Integer status;           // 0=disabled, 1=enabled
    private LocalDateTime lastExecTime;
    private LocalDateTime nextExecTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    @TableLogic
    private Integer deleted;

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public String getReplyType() { return replyType; }
    public void setReplyType(String replyType) { this.replyType = replyType; }

    public String getReplyContent() { return replyContent; }
    public void setReplyContent(String replyContent) { this.replyContent = replyContent; }

    public String getReplyMarkup() { return replyMarkup; }
    public void setReplyMarkup(String replyMarkup) { this.replyMarkup = replyMarkup; }

    public String getParseMode() { return parseMode; }
    public void setParseMode(String parseMode) { this.parseMode = parseMode; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getLastExecTime() { return lastExecTime; }
    public void setLastExecTime(LocalDateTime lastExecTime) { this.lastExecTime = lastExecTime; }

    public LocalDateTime getNextExecTime() { return nextExecTime; }
    public void setNextExecTime(LocalDateTime nextExecTime) { this.nextExecTime = nextExecTime; }

    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }

    public LocalDateTime getGmtModified() { return gmtModified; }
    public void setGmtModified(LocalDateTime gmtModified) { this.gmtModified = gmtModified; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
