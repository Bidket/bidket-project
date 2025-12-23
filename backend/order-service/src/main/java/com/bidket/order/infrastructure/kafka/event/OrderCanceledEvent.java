package com.bidket.order.infrastructure.kafka.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class OrderCanceledEvent {

    private static final String SOURCE = "order-service";
    private static final String TYPE = "ORDER_CANCELED";

    public static StandardEvent create(
            UUID orderId,
            UUID auctionId,
            UUID userId,
            String reason,
            UUID correlationId
    ) {
        if (orderId == null) throw new IllegalArgumentException("orderId는 필수입니다");
        if (auctionId == null) throw new IllegalArgumentException("auctionId는 필수입니다");
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason은 필수입니다");
        if (correlationId == null) throw new IllegalArgumentException("correlationId는 필수입니다");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", orderId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("reason", reason);
        data.put("correlationId", correlationId.toString());

        return StandardEvent.of(SOURCE, TYPE, userId, data);
    }
}
