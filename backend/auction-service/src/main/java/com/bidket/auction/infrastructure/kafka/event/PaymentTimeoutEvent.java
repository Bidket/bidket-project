package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 타임아웃 이벤트
 * Order Service에서 발행
 *
 * 역할:
 * - 주문 생성 후 30분 이내 결제 미완료 시 발행
 * - Auction Service의 PaymentTimeoutSagaOrchestrator를 트리거
 */
public record PaymentTimeoutEvent(
        UUID eventId,
        UUID orderId,
        UUID auctionId,
        UUID winnerId,
        UUID productSizeId,
        Long amount,
        String reason,
        LocalDateTime timeoutAt,
        UUID correlationId,
        LocalDateTime createdAt
) {
    public static PaymentTimeoutEvent create(
            UUID orderId,
            UUID auctionId,
            UUID winnerId,
            UUID productSizeId,
            Long amount,
            String reason,
            UUID correlationId
    ) {
        return new PaymentTimeoutEvent(
                UUID.randomUUID(),
                orderId,
                auctionId,
                winnerId,
                productSizeId,
                amount,
                reason,
                LocalDateTime.now(),
                correlationId,
                LocalDateTime.now()
        );
    }
}
