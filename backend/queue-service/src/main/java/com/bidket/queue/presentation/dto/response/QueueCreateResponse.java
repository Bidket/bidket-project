package com.bidket.queue.presentation.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record QueueCreateResponse(
        UUID auctionId,
        Long maxActive,
        Integer permitsPerSec,
        LocalDateTime openAt,
        LocalDateTime closeAt
) {
}
