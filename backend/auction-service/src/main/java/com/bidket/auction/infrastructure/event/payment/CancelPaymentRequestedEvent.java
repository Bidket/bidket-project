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

        return new StandardEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                "auction-service",
                "CANCEL_PAYMENT_REQUESTED",
                data
        );
    }

    public static StandardEvent createWithUser(
            UUID paymentId,
            UUID orderId,
            UUID auctionId,
            UUID userId,
            String reason,
            UUID correlationId
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId.toString());  
        data.put("paymentId", paymentId.toString());
        data.put("orderId", orderId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("reason", reason);
        data.put("correlationId", correlationId.toString());
        data.put("requestedAt", LocalDateTime.now().toString());

        return new StandardEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                "auction-service",
                "CANCEL_PAYMENT_REQUESTED",
                data
        );
    }
}
