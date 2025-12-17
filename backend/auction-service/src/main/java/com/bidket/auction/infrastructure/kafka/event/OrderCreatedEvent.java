package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Order Service로부터 주문 생성 성공 결과를 수신하는 이벤트
 * Topic: auction.order (Auction이 소비, Order가 생산)
 * Type: ORDER_CREATED
 *
 * 표준 이벤트 구조:
 * - eventId: 멱등성 체크용
 * - occurredAt: 이벤트 발생 시각
 * - source: "order-service"
 * - type: "ORDER_CREATED"
 * - userId: 주문자 ID
 * - data: 핵심 정보만 포함
 *   - orderId: 생성된 주문 ID
 *   - sagaId: Saga 추적 ID
 *   - auctionId: 경매 ID
 *   - productSizeId: 상품 사이즈 ID
 *   - amount: 주문 금액
 *   - paymentDeadline: 결제 기한
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
            UUID productSizeId,
            Long amount,
            LocalDateTime paymentDeadline,
            UUID correlationId
    ) {
        if (orderId == null) throw new IllegalArgumentException("orderId must not be null");
        if (sagaId == null) throw new IllegalArgumentException("sagaId must not be null");
        if (auctionId == null) throw new IllegalArgumentException("auctionId must not be null");
        if (userId == null) throw new IllegalArgumentException("userId must not be null");
        if (productSizeId == null) throw new IllegalArgumentException("productSizeId must not be null");
        if (amount == null || amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (paymentDeadline == null) throw new IllegalArgumentException("paymentDeadline must not be null");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", orderId.toString());
        data.put("sagaId", sagaId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("productSizeId", productSizeId.toString());
        data.put("amount", amount);
        data.put("paymentDeadline", paymentDeadline.toString());
        if (correlationId != null) {
            data.put("correlationId", correlationId.toString());
        }

        return StandardEvent.of(SOURCE, TYPE, userId, data);
    }
}
