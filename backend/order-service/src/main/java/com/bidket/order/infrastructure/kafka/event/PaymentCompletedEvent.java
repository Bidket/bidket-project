package com.bidket.order.infrastructure.kafka.event;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PaymentCompletedEvent {

    private static final String SOURCE = "order-service";
    private static final String TYPE = "PAYMENT_COMPLETED";

    public static StandardEvent create(
            UUID orderId,
            UUID paymentId,
            UUID auctionId,
            UUID userId,
            Long amount,
            UUID correlationId
    ) {
        if (orderId == null) throw new IllegalArgumentException("orderId는 필수입니다");
        if (paymentId == null) throw new IllegalArgumentException("paymentId는 필수입니다");
        if (auctionId == null) throw new IllegalArgumentException("auctionId는 필수입니다");
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다");
        if (amount == null || amount <= 0) throw new IllegalArgumentException("amount는 양수여야 합니다");
        if (correlationId == null) throw new IllegalArgumentException("correlationId는 필수입니다");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", orderId.toString());
        data.put("paymentId", paymentId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("amount", amount);
        data.put("correlationId", correlationId.toString());

        return StandardEvent.of(SOURCE, TYPE, userId, data);
    }
}
