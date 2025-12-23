package com.bidket.auction.infrastructure.event.payment;

import com.bidket.auction.infrastructure.kafka.event.StandardEvent;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record CancelPaymentRequestedEvent() {

    public static StandardEvent create(
            UUID paymentId,
            UUID orderId,
            UUID auctionId,
            UUID userId,
            String reason,
            UUID correlationId
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentId", paymentId.toString());
        data.put("orderId", orderId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("reason", reason);
        data.put("correlationId", correlationId.toString());
        data.put("requestedAt", LocalDateTime.now().toString());

        return StandardEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("CANCEL_PAYMENT_REQUESTED")
                .occurredAt(LocalDateTime.now())
                .source("auction-service")
                .userId(userId)
                .data(data)
                .build();
    }
}
