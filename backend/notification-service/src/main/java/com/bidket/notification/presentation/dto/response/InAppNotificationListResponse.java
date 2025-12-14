package com.bidket.notification.presentation.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 인앱 알림 목록 응답 DTO
 */
@Builder
public record InAppNotificationListResponse(
        List<InAppNotificationResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}

