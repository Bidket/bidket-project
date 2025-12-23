package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record CancelOrderRequestedEvent() {

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
                data
        );
    }
}
