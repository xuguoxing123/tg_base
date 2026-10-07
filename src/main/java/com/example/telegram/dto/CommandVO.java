package com.example.telegram.dto;

import java.time.LocalDateTime;

public record CommandVO(
        Long id,
        String name,
        String displayName,
        String description,
        String replyType,
        String replyContent,
        String replyMarkup,
        Integer status,
        Integer sortOrder,
        LocalDateTime gmtCreate,
        LocalDateTime gmtModified
) {
}
