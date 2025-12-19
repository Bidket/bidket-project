package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 주문 취소 요청 이벤트
 * Auction Service → Order Service
 *
 * 발행 시나리오:
 * - 결제 타임아웃으로 경매 재오픈 시 해당 주문 취소
 * - 보상 트랜잭션 실행 시
 */
public record CancelOrderRequestedEvent() {

    /**
     * StandardEvent 형식으로 주문 취소 요청 이벤트 생성
     *
     * @param orderId 취소할 주문 ID
     * @param auctionId 경매 ID
     * @param reason 취소 사유
     * @param correlationId 상관 ID
     * @return StandardEvent
     */
    public static StandardEvent create(
            UUID orderId,
            UUID auctionId,
            String reason,
            UUID correlationId
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", orderId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("reason", reason);
        data.put("correlationId", correlationId.toString());

        return new StandardEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                "auction-service",
                "CANCEL_ORDER_REQUESTED",
                null, // userId는 시스템 이벤트이므로 null
                data
        );
    }
}
