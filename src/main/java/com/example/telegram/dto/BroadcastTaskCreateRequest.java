package com.example.telegram.dto;

public record BroadcastTaskCreateRequest(
        String name,
        String description,
        Long chatId,
        String cronExpression,
        String replyType,
        String replyContent,
        String replyMarkup,
        String parseMode,
        Integer status
) {
}
