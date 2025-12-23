package com.bidket.order.infrastructure.kafka.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 주문 생성 완료 이벤트
 * Topic: auction.order (Auction이 소비, Order가 생산)
 * Type: ORDER_CREATED
 *
 * 표준 이벤트 구조 (팀 합의안):
 * - eventId: 멱등성 체크용
 * - occurredAt: 이벤트 발생 시각
 * - source: "order-service"
 * - eventType: "ORDER_CREATED"
 * - data: 핵심 정보만 포함
 *   - userId: 주문자 ID (data 내부로 이동)
 *   - orderId: 생성된 주문 ID
 *   - sagaId: Saga 추적 ID
 *   - auctionId: 경매 ID
 *   - correlationId: 상관 ID (Saga 추적)
 */
public class OrderCreatedEvent {

    private static final String SOURCE = "order-service";
    private static final String TYPE = "ORDER_CREATED";

    public static StandardEvent create(
            UUID orderId,
            UUID sagaId,
            UUID auctionId,
            UUID userId,
            UUID correlationId
    ) {
        if (orderId == null) throw new IllegalArgumentException("orderId는 필수입니다");
        if (sagaId == null) throw new IllegalArgumentException("sagaId는 필수입니다");
        if (auctionId == null) throw new IllegalArgumentException("auctionId는 필수입니다");
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다");
        if (correlationId == null) throw new IllegalArgumentException("correlationId는 필수입니다");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId.toString()); // userId를 data 내부로 이동
        data.put("orderId", orderId.toString());
        data.put("sagaId", sagaId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("correlationId", correlationId.toString());

        return StandardEvent.of(SOURCE, TYPE, data);
    }
}
