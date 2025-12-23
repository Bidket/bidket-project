package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class OrderCreatedEvent {

    private static final String SOURCE = "order-service";
    private static final String TYPE = "ORDER_CREATED";

    public static StandardEvent create(
            UUID orderId,
            UUID sagaId,
            UUID auctionId,
            UUID userId,
            UUID productSizeId,
            Long amount,
            LocalDateTime paymentDeadline,
            UUID correlationId
    ) {
        if (orderId == null) throw new IllegalArgumentException("orderId must not be null");
        if (sagaId == null) throw new IllegalArgumentException("sagaId must not be null");
        if (auctionId == null) throw new IllegalArgumentException("auctionId must not be null");
        if (userId == null) throw new IllegalArgumentException("userId must not be null");
        if (productSizeId == null) throw new IllegalArgumentException("productSizeId must not be null");
        if (amount == null || amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (paymentDeadline == null) throw new IllegalArgumentException("paymentDeadline must not be null");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId.toString());  
        data.put("orderId", orderId.toString());
        data.put("sagaId", sagaId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("productSizeId", productSizeId.toString());
        data.put("amount", amount);
        data.put("paymentDeadline", paymentDeadline.toString());
        if (correlationId != null) {
            data.put("correlationId", correlationId.toString());
        }

        return StandardEvent.of(SOURCE, TYPE, data);
    }
}
