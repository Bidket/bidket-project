package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class AuctionClosedEvent {

    private AuctionClosedEvent() {
        throw new UnsupportedOperationException("Factory class");
    }

    public static StandardEvent createWinnerEvent(
            UUID winnerId,
            UUID auctionId,
            Long finalPrice,
            LocalDateTime closedAt
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("auctionId", auctionId.toString());
        data.put("result", "WON");
        data.put("finalPrice", finalPrice);
        data.put("closedAt", closedAt.toString());

        return StandardEvent.of(
                "auction-service",
                "closed",
                winnerId,
                data
        );
    }

    public static StandardEvent createLoserEvent(
            UUID loserId,
            UUID auctionId,
            Long finalPrice,
            LocalDateTime closedAt
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("auctionId", auctionId.toString());
        data.put("result", "LOST");
        data.put("finalPrice", finalPrice);
        data.put("closedAt", closedAt.toString());

        return StandardEvent.of(
                "auction-service",
                "closed",
                loserId,
                data
        );
    }
}
