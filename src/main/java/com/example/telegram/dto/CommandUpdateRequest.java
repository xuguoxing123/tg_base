package com.example.telegram.dto;

public record CommandUpdateRequest(
        Long id,
        String name,
        String displayName,
        String description,
        String replyType,
        String replyContent,
        String replyMarkup,
        Integer status
) {
}
