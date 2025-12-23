package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CreateOrderRequestedEvent {

    private static final String SOURCE = "auction-service";
    private static final String TYPE = "CREATE_ORDER_REQUESTED";

    public static StandardEvent create(
            UUID sagaId,
            UUID auctionId,
            UUID winnerUserId,
            UUID productSizeId,
            Long price,
            UUID correlationId
    ) {
        if (sagaId == null) throw new IllegalArgumentException("sagaId must not be null");
        if (auctionId == null) throw new IllegalArgumentException("auctionId must not be null");
        if (winnerUserId == null) throw new IllegalArgumentException("winnerUserId must not be null");
        if (productSizeId == null) throw new IllegalArgumentException("productSizeId must not be null");
        if (price == null || price <= 0) throw new IllegalArgumentException("price must be positive");
        if (correlationId == null) throw new IllegalArgumentException("correlationId must not be null");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sagaId", sagaId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("productSizeId", productSizeId.toString());
        data.put("price", price);
        data.put("paymentDeadline", LocalDateTime.now().plusMinutes(30).toString());
        data.put("correlationId", correlationId.toString());

        return StandardEvent.of(SOURCE, TYPE, winnerUserId, data);
    }
}
