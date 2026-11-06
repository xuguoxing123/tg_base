package com.example.telegram.dto;

import java.time.LocalDateTime;

/**
 * 用户管理列表视图对象。
 */
public record TelegramUserVO(
        Long id,
        Long userId,
        String username,
        String firstName,
        String lastName,
        String role,
        Integer status,
        LocalDateTime firstSeenTime,
        LocalDateTime lastActiveTime,
        Integer messageCount
) {
}
