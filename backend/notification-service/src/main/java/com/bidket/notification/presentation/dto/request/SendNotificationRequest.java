package com.bidket.notification.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.Map;
import java.util.UUID;

/**
 * 알림 단건 발송 요청 DTO
 */
@Builder
@Schema(description = "알림 단건 발송 요청")
public record SendNotificationRequest(
        @NotBlank(message = "알림 타입은 필수입니다.")
        @Schema(description = "알림 타입", example = "SLACK", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"EMAIL", "SMS", "IN_APP", "SLACK"})
        String type, // EMAIL, SMS, IN_APP, SLACK

        @Schema(description = "대상 회원 ID (EMAIL, IN_APP 타입일 때 필수, SLACK 타입일 때 선택)", example = "7c4d3a1b-2f9d-4c62-9b4a-1d2f34e5a678")
        UUID userId, // 대상 회원 ID (EMAIL, IN_APP일 때 필수, SLACK일 때 선택)

        @NotBlank(message = "알림 제목은 필수입니다.")
        @Schema(description = "알림 제목", example = "경매 시작 알림", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @NotBlank(message = "알림 내용은 필수입니다.")
        @Schema(description = "알림 내용", example = "새로운 경매가 시작되었습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        String message,

        @Schema(description = "알림 타입별 대상 주소: EMAIL 타입일 경우 수신자 이메일 주소 (필수), SLACK 타입일 경우 무시됨 (항상 기본 webhook URL 사용)", example = "user@example.com")
        String target, // EMAIL 타입일 경우 이메일 주소, SLACK 타입일 경우 무시됨

        @Schema(description = "알림 카테고리", example = "AUCTION_START", allowableValues = {"AUCTION_START", "BID_SUCCESS", "PAYMENT_EXPIRE", "QUEUE_CALL", "PAYMENT_DONE", "SYSTEM"})
        String category, // 알림 카테고리 (AUCTION_START, BID_SUCCESS, PAYMENT_EXPIRE 등, 선택사항)

        @Schema(description = "추가 데이터(JSON)", example = "{\"auctionId\":\"550e8400-e29b-41d4-a716-446655440000\",\"shoeId\":\"660e8400-e29b-41d4-a716-446655440001\"}")
        Map<String, Object> payload, // 추가 데이터(JSON), 클라이언트에서 딥링크/상세페이지 이동 등에 사용 (선택사항)

        @Schema(description = "링크 URL", example = "https://bidket.com/auctions/550e8400-e29b-41d4-a716-446655440000")
        String linkUrl, // 링크 URL (선택사항)

        @Schema(description = "발송 예약 시각 (ISO-8601)", example = "2024-12-10T10:00:00Z")
        String sendAt // 지정 시각 발송 예약 (ISO-8601 형식, 선택사항, 미구현 시 즉시 발송 처리)
) {
}

