package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record AuctionReopenedEvent() {

    public static StandardEvent create(
            UUID auctionId,
            UUID productSizeId,
            UUID previousWinnerId,
            String reason,
            LocalDateTime newEndTime,
            UUID correlationId
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("auctionId", auctionId.toString());
        data.put("productSizeId", productSizeId.toString());
        data.put("previousWinnerId", previousWinnerId.toString());
        data.put("reason", reason);
        data.put("newEndTime", newEndTime.toString());
        data.put("correlationId", correlationId.toString());

        return StandardEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("AUCTION_REOPENED")
                .occurredAt(LocalDateTime.now())
                .source("auction-service")
                .userId(previousWinnerId)
                .data(data)
                .build();
    }
}
