package com.bidket.queue.domain.model;

import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import lombok.Builder;
import lombok.Getter;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public class QueueConfigModel {
    private UUID auctionId;
    @Getter
    private Long maxActive;
    private Integer permitsPerSec;
    private Instant openAt;
    private Instant closeAt;

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

    public void checkOpenStatus(Instant now) {
        if(now.isBefore(openAt))
            throw new QueueException(QueueErrorCode.AUCTION_NOT_OPENED);
        if(now.isAfter(closeAt))
            throw new QueueException(QueueErrorCode.AUCTION_CLOSED);
    }
}
