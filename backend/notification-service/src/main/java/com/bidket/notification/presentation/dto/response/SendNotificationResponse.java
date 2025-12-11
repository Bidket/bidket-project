package com.bidket.notification.presentation.dto.response;

import lombok.Builder;

import java.util.UUID;

/**
 * 알림 단건 발송 응답 DTO
 */
@Builder
public record SendNotificationResponse(
        UUID notificationId,  // 알림 ID (UUID)
        String status,        // 발송 상태 (SENT, QUEUED, FAILED)
        String type,          // 요청된 알림 타입
        String message        // 실패 시 에러 메시지 또는 발송 결과 요약 (선택사항)
) {
}

