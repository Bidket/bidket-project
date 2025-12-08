package com.bidket.queue.presentation.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record QueueCreateResponse(
        UUID auctionId,
        Long maxActive,
        Integer permitsPerSec,
        Instant openAt,
        Instant closeAt
) {
}
