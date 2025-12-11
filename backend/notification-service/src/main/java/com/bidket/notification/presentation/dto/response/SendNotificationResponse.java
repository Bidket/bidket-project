package com.bidket.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 알림 단건 발송 응답 DTO
 */
@Builder
@Schema(description = "알림 단건 발송 응답")
public record SendNotificationResponse(
        @Schema(description = "알림 ID (UUID)", example = "3d0d0fa8-5c24-4f0a-8a0e-2b6ce1cb2f90")
        String notificationId,
        
        @Schema(description = "발송 상태", example = "SENT", allowableValues = {"SENT", "QUEUED", "FAILED"})
        String status,
        
        @Schema(description = "요청된 알림 타입", example = "SLACK")
        String type,
        
        @Schema(description = "발송 결과 메시지", example = "알림이 정상적으로 발송되었습니다.")
        String message
) {
}

