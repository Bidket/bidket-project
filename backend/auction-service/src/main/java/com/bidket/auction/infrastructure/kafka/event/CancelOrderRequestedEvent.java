package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record CancelOrderRequestedEvent() {

    public static StandardEvent create(
            UUID orderId,
            UUID auctionId,
            UUID userId,
            String reason,
            UUID correlationId
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", orderId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("reason", reason);
        data.put("correlationId", correlationId.toString());

        return StandardEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("CANCEL_ORDER_REQUESTED")
                .occurredAt(LocalDateTime.now())
                .source("auction-service")
                .userId(userId)
                .data(data)
                .build();
    }
}
