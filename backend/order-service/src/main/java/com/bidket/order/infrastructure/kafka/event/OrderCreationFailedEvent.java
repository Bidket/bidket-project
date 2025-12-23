package com.bidket.order.infrastructure.kafka.event;

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
            String reason,
            UUID correlationId
    ) {
        if (sagaId == null) throw new IllegalArgumentException("sagaId는 필수입니다");
        if (auctionId == null) throw new IllegalArgumentException("auctionId는 필수입니다");
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason은 필수입니다");
        if (correlationId == null) throw new IllegalArgumentException("correlationId는 필수입니다");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sagaId", sagaId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("reason", reason);
        data.put("correlationId", correlationId.toString());

        return StandardEvent.of(SOURCE, TYPE, userId, data);
    }
}
