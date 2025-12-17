package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Order Service에 주문 생성을 요청하는 이벤트
 * Topic: order.auction (Order가 소비, Auction이 생산)
 * Type: CREATE_ORDER_REQUESTED
 *
 * 표준 이벤트 구조:
 * - eventId: 멱등성 체크용
 * - occurredAt: 이벤트 발생 시각
 * - source: "auction-service"
 * - type: "CREATE_ORDER_REQUESTED"
 * - userId: 낙찰자 ID
 * - data: 핵심 정보만 포함
 *   - sagaId: Saga 추적 ID
 *   - auctionId: 경매 ID
 *   - productSizeId: 상품 사이즈 ID
 *   - price: 낙찰 금액
 *   - paymentDeadline: 결제 기한
 *   - correlationId: 상관 ID (Saga 추적)
 */
public class CreateOrderRequestedEvent {

    private static final String SOURCE = "auction-service";
    private static final String TYPE = "CREATE_ORDER_REQUESTED";

    public static StandardEvent create(
            UUID sagaId,
            UUID auctionId,
            UUID winnerUserId,
            UUID productSizeId,
            Long price,
            UUID correlationId
    ) {
        if (sagaId == null) throw new IllegalArgumentException("sagaId must not be null");
        if (auctionId == null) throw new IllegalArgumentException("auctionId must not be null");
        if (winnerUserId == null) throw new IllegalArgumentException("winnerUserId must not be null");
        if (productSizeId == null) throw new IllegalArgumentException("productSizeId must not be null");
        if (price == null || price <= 0) throw new IllegalArgumentException("price must be positive");
        if (correlationId == null) throw new IllegalArgumentException("correlationId must not be null");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sagaId", sagaId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("productSizeId", productSizeId.toString());
        data.put("price", price);
        data.put("paymentDeadline", LocalDateTime.now().plusMinutes(30).toString());
        data.put("correlationId", correlationId.toString());

        return StandardEvent.of(SOURCE, TYPE, winnerUserId, data);
    }
}
