package com.bidket.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 알림 읽음 처리 응답 DTO
 */
@Builder
@Schema(description = "알림 읽음 처리 응답")
public record ReadNotificationResponse(
        @Schema(description = "알림 ID", example = "e32e8f1e-8244-4f0c-9dc8-71d51390ac01", requiredMode = Schema.RequiredMode.REQUIRED)
        String notificationId,

        @Schema(description = "읽음 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean read,

        @Schema(description = "읽은 일시", example = "2025-11-26T13:10:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime readAt
) {
}

