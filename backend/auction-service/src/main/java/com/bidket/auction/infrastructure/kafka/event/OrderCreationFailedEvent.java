package com.bidket.auction.infrastructure.kafka.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Order Service로부터 주문 생성 실패 결과를 수신하는 이벤트
 * Topic: auction.order (Auction이 소비, Order가 생산)
 * Type: ORDER_CREATION_FAILED
 *
 * 표준 이벤트 구조:
 * - eventId: 멱등성 체크용
 * - occurredAt: 이벤트 발생 시각 (실패 시각)
 * - source: "order-service"
 * - type: "ORDER_CREATION_FAILED"
 * - userId: 주문자 ID
 * - data: 핵심 정보만 포함
 *   - sagaId: Saga 추적 ID
 *   - auctionId: 경매 ID
 *   - failureReason: 실패 사유 (한글 설명)
 *   - failureCode: 실패 코드 (STOCK_UNAVAILABLE 등)
 *   - retryable: 재시도 가능 여부
 *   - correlationId: 상관 ID (Saga 추적)
 */
public class OrderCreationFailedEvent {

    private static final String SOURCE = "order-service";
    private static final String TYPE = "ORDER_CREATION_FAILED";

    public static StandardEvent create(
            UUID sagaId,
            UUID auctionId,
            UUID userId,
            String failureReason,
            String failureCode,
            boolean retryable,
            UUID correlationId
    ) {
        if (sagaId == null) throw new IllegalArgumentException("sagaId must not be null");
        if (auctionId == null) throw new IllegalArgumentException("auctionId must not be null");
        if (userId == null) throw new IllegalArgumentException("userId must not be null");
        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("failureReason must not be blank");
        }
        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("failureCode must not be blank");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sagaId", sagaId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("failureReason", failureReason);
        data.put("failureCode", failureCode);
        data.put("retryable", retryable);
        if (correlationId != null) {
            data.put("correlationId", correlationId.toString());
        }

        return StandardEvent.of(SOURCE, TYPE, userId, data);
    }
}
