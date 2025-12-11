package com.bidket.notification.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.Map;
import java.util.UUID;

/**
 * 알림 단건 발송 요청 DTO
 */
@Builder
public record SendNotificationRequest(
        @NotBlank(message = "알림 타입은 필수입니다.")
        String type, // EMAIL, SMS, IN_APP, SLACK

        UUID userId, // 대상 회원 ID (선택사항, Slack 시스템 알림의 경우 없을 수 있음)

        @NotBlank(message = "알림 제목은 필수입니다.")
        String title,

        @NotBlank(message = "알림 내용은 필수입니다.")
        String message,

        String target, // 이메일 주소, 전화번호 등 직접 발송 대상 (회원 미연동 알림용, 선택사항)

        String category, // 알림 카테고리 (AUCTION_START, BID_SUCCESS, PAYMENT_EXPIRE 등, 선택사항)

        Map<String, Object> payload, // 추가 데이터(JSON), 클라이언트에서 딥링크/상세페이지 이동 등에 사용 (선택사항)

        String linkUrl, // 링크 URL (선택사항)

        String sendAt // 지정 시각 발송 예약 (ISO-8601 형식, 선택사항, 미구현 시 즉시 발송 처리)
) {
}

