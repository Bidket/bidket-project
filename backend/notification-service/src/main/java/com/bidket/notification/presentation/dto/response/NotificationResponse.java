package com.bidket.notification.presentation.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 알림 응답 DTO
 */
@Builder
public record NotificationResponse(
        UUID notificationId,
        UUID userId,
        String type,
        String channel,
        String title,
        String message,
        String status,
        LocalDateTime sentAt,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
}

