package com.bidket.order.infrastructure.kafka.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 주문 생성 실패 이벤트
 * Topic: auction.order (Auction이 소비, Order가 생산)
 * Type: ORDER_CREATION_FAILED
 *
 * 표준 이벤트 구조:
 * - eventId: 멱등성 체크용
 * - occurredAt: 이벤트 발생 시각
 * - source: "order-service"
 * - eventType: "ORDER_CREATION_FAILED"
 * - userId: 주문자 ID
 * - data: 핵심 정보만 포함
 *   - sagaId: Saga 추적 ID
 *   - auctionId: 경매 ID
 *   - reason: 실패 사유
 *   - correlationId: 상관 ID (Saga 추적)
 */
public class OrderCreationFailedEvent {

    private static final String SOURCE = "order-service";
    private static final String TYPE = "ORDER_CREATION_FAILED";

    public static StandardEvent create(
            UUID sagaId,
            UUID auctionId,
            UUID userId,
            String reason,
            UUID correlationId
    ) {
        if (sagaId == null) throw new IllegalArgumentException("sagaId는 필수입니다");
        if (auctionId == null) throw new IllegalArgumentException("auctionId는 필수입니다");
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason은 필수입니다");
        if (correlationId == null) throw new IllegalArgumentException("correlationId는 필수입니다");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sagaId", sagaId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("reason", reason);
        data.put("correlationId", correlationId.toString());

        return StandardEvent.of(SOURCE, TYPE, userId, data);
    }
}
