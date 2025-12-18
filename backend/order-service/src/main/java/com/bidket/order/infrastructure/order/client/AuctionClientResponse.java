package com.bidket.order.infrastructure.order.client;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuctionClientResponse(
        UUID auctionId,
        UUID productSizeId,
        UUID sellerId,
        String auctionTitle,
        LocalDateTime startTime,
        String status,
        UUID winnerId,
        Long finalPrice
) {

}