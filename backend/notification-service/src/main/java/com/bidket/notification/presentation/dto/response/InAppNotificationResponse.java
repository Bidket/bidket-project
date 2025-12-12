package com.bidket.notification.presentation.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 인앱 알림 응답 DTO
 */
@Builder
public record InAppNotificationResponse(
        String notificationId,
        String title,
        String message,
        String category,
        String linkUrl,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}

