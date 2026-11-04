package com.example.telegram.dto;

import java.time.LocalDateTime;

public record BroadcastTaskVO(
        Long id,
        String name,
        String description,
        Long chatId,
        String cronExpression,
        String replyType,
        String replyContent,
        String replyMarkup,
        String parseMode,
        Integer status,
        LocalDateTime lastExecTime,
        LocalDateTime nextExecTime,
        LocalDateTime gmtCreate,
        LocalDateTime gmtModified
) {
}
