package com.example.telegram.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("cs_bot_command")
public class BotCommandEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String displayName;
    private String description;
    private String replyType;
    private String replyContent;
    private String replyMarkup;   // JSON string stored in DB
    private Integer status;        // 0=disabled, 1=enabled
    private String visibleUserIds; // JSON array or null
    private Integer sortOrder;

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

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReplyType() { return replyType; }
    public void setReplyType(String replyType) { this.replyType = replyType; }

    public String getReplyContent() { return replyContent; }
    public void setReplyContent(String replyContent) { this.replyContent = replyContent; }

    public String getReplyMarkup() { return replyMarkup; }
    public void setReplyMarkup(String replyMarkup) { this.replyMarkup = replyMarkup; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getVisibleUserIds() { return visibleUserIds; }
    public void setVisibleUserIds(String visibleUserIds) { this.visibleUserIds = visibleUserIds; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }

    public LocalDateTime getGmtModified() { return gmtModified; }
    public void setGmtModified(LocalDateTime gmtModified) { this.gmtModified = gmtModified; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
