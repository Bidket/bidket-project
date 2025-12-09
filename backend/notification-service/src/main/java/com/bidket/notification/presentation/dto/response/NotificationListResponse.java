package com.bidket.notification.presentation.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 알림 목록 응답 DTO
 */
@Builder
public record NotificationListResponse(
        List<NotificationResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}

