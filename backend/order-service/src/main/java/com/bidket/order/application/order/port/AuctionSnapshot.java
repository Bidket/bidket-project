package com.bidket.order.application.order.port;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuctionSnapshot(
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