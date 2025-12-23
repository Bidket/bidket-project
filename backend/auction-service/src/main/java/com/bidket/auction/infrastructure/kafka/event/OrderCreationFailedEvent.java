package com.bidket.auction.infrastructure.kafka.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class OrderCreationFailedEvent {

    private static final String SOURCE = "order-service";
    private static final String TYPE = "ORDER_CREATION_FAILED";

    public static StandardEvent create(
            UUID sagaId,
            UUID auctionId,
            UUID userId,
            String failureReason,
            String failureCode,
            boolean retryable,
            UUID correlationId
    ) {
        if (sagaId == null) throw new IllegalArgumentException("sagaId must not be null");
        if (auctionId == null) throw new IllegalArgumentException("auctionId must not be null");
        if (userId == null) throw new IllegalArgumentException("userId must not be null");
        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("failureReason must not be blank");
        }
        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("failureCode must not be blank");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId.toString());  
        data.put("sagaId", sagaId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("failureReason", failureReason);
        data.put("failureCode", failureCode);
        data.put("retryable", retryable);
        if (correlationId != null) {
            data.put("correlationId", correlationId.toString());
        }

        return StandardEvent.of(SOURCE, TYPE, data);
    }
}
