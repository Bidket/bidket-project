package com.bidket.queue.presentation.dto.response;

import com.bidket.queue.domain.model.QueueStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.util.UUID;

@Builder
public record QueueStatusResponse(
        UUID auctionId,
        UUID userId,
        QueueStatus status,
        Long rank,
        Integer retryAfter,
        @JsonIgnore
        String token,
        String message
) {
}
