package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

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
