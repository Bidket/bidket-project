package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 경매 재오픈 이벤트
 * Auction Service에서 발행
 *
 * 역할:
 * - 결제 타임아웃으로 경매가 재오픈되었음을 알림
 * - Notification Service에서 소비하여 판매자 및 2등 입찰자에게 알림
 */
public record AuctionReopenedEvent(
        UUID eventId,
        UUID auctionId,
        UUID productSizeId,
        UUID previousWinnerId,
        String reason,
        LocalDateTime newEndTime,
        UUID correlationId,
        LocalDateTime createdAt
) {
    public static AuctionReopenedEvent create(
            UUID auctionId,
            UUID productSizeId,
            UUID previousWinnerId,
            String reason,
            LocalDateTime newEndTime,
            UUID correlationId
    ) {
        return new AuctionReopenedEvent(
                UUID.randomUUID(),
                auctionId,
                productSizeId,
                previousWinnerId,
                reason,
                newEndTime,
                correlationId,
                LocalDateTime.now()
        );
    }
}
