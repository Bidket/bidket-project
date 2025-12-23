package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class OutbidEvent {

    private OutbidEvent() {
        throw new UnsupportedOperationException("Factory class");
    }

    public static StandardEvent create(
            UUID previousBidderId,
            UUID auctionId,
            Long currentPrice,
            LocalDateTime outbidAt
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", previousBidderId.toString());  
        data.put("auctionId", auctionId.toString());
        data.put("currentPrice", currentPrice);
        data.put("outbidAt", outbidAt.toString());

        return StandardEvent.of(
                "auction-service",
                "outbid",
                data
        );
    }
}
