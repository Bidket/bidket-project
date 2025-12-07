package com.bidket.queue.presentation.dto.response;

import com.bidket.queue.domain.model.QueueConfigModel;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record QueueConfigUpdateResponse(
        UUID auctionId,
        Long maxActive,
        Integer permitsPerSec,
        Instant openAt,
        Instant closeAt
) {
}
