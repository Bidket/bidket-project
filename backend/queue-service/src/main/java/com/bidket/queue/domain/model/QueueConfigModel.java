package com.bidket.queue.domain.model;

import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public class QueueConfigModel {
    private UUID auctionId;
    private Long maxActive;
    private Integer permitsPerSec;
    private LocalDateTime openAt;
    private LocalDateTime closeAt;

    public Map<String, Object> toMap() {
        return Map.of("auctionId", auctionId,
                "max_active", maxActive,
                "permits_per_sec", permitsPerSec,
                "open_at", openAt.toString(),
                "close_at", closeAt.toString());
    }

    public QueueCreateResponse toCreateResponse() {
        return QueueCreateResponse.builder()
                .auctionId(auctionId)
                .maxActive(maxActive)
                .permitsPerSec(permitsPerSec)
                .openAt(openAt)
                .closeAt(closeAt)
                .build();
    }
}
