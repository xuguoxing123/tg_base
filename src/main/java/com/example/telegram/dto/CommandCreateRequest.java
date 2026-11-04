package com.example.telegram.dto;

public record CommandCreateRequest(
        String name,
        String displayName,
        String description,
        String replyType,
        String replyContent,
        String replyMarkup,
        String parseMode,
        String triggerKeywords,
        String welcomeText,
        Integer status
) {
}
