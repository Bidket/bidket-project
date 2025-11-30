package com.bidket.queue.presentation.dto.request;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public record QueueCreateRequest(
        UUID auctionId,
        Long maxActive,
        Integer permitsPerSec,
        LocalDateTime openAt,
        LocalDateTime closeAt
) {
    public Map<String, Object> toMap() {
        return Map.of("auctionId", auctionId,
                "max_active", maxActive,
                "permits_per_sec", permitsPerSec,
                "open_at", openAt.toString(),
                "close_at", closeAt.toString());
    }
}
