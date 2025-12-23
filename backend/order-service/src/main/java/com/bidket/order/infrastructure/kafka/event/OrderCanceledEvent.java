package com.bidket.order.infrastructure.kafka.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 주문 취소 이벤트
 * Topic: auction.order (Auction이 소비, Order가 생산)
 * Type: ORDER_CANCELED
 *
 * 표준 이벤트 구조:
 * - eventId: 멱등성 체크용
 * - occurredAt: 이벤트 발생 시각
 * - source: "order-service"
 * - eventType: "ORDER_CANCELED"
 * - userId: 주문자 ID
 * - data: 핵심 정보만 포함
 *   - orderId: 주문 ID
 *   - auctionId: 경매 ID
 *   - reason: 취소 사유
 *   - correlationId: 상관 ID (Saga 추적)
 */
public class OrderCanceledEvent {

    private static final String SOURCE = "order-service";
    private static final String TYPE = "ORDER_CANCELED";

    public static StandardEvent create(
            UUID orderId,
            UUID auctionId,
            UUID userId,
            String reason,
            UUID correlationId
    ) {
        if (orderId == null) throw new IllegalArgumentException("orderId는 필수입니다");
        if (auctionId == null) throw new IllegalArgumentException("auctionId는 필수입니다");
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason은 필수입니다");
        if (correlationId == null) throw new IllegalArgumentException("correlationId는 필수입니다");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId.toString()); // userId를 data에 포함
        data.put("orderId", orderId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("reason", reason);
        data.put("correlationId", correlationId.toString());

        return StandardEvent.of(SOURCE, TYPE, data);
    }
}
