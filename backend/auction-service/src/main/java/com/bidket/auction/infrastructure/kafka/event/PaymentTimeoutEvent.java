package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PaymentTimeoutEvent {

    private static final String SOURCE = "auction-service";
    private static final String TYPE = "PAYMENT_TIMEOUT";

    private PaymentTimeoutEvent() {
        throw new UnsupportedOperationException("Factory class");
    }

    public static StandardEvent create(
            UUID orderId,
            UUID auctionId,
            UUID winnerId,
            UUID productSizeId,
            Long amount,
            String reason,
            UUID correlationId
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", orderId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("productSizeId", productSizeId.toString());
        data.put("amount", amount);
        data.put("reason", reason);
        data.put("timeoutAt", LocalDateTime.now().toString());
        if (correlationId != null) {
            data.put("correlationId", correlationId.toString());
        }

        return StandardEvent.of(SOURCE, TYPE, winnerId, data);
    }
}
