package com.bidket.notification.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

/**
 * 알림 생성 요청 DTO
 */
@Builder
public record CreateNotificationRequest(
        @NotNull(message = "회원 ID는 필수입니다.")
        UUID userId,

        @NotBlank(message = "알림 유형은 필수입니다.")
        String type, // QUEUE_CALL, BID_SUCCESS, PAYMENT_DONE 등

        @NotBlank(message = "알림 제목은 필수입니다.")
        String title,

        @NotBlank(message = "알림 내용은 필수입니다.")
        String message,

        String channel // PUSH, EMAIL, SMS, SLACK, SYSTEM (선택사항, 기본값: PUSH)
) {
}

