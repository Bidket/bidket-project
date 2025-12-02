package com.bidket.queue.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.util.UUID;

@Builder
public record QueueEnterResponse(
        UUID auctionId,
        UUID userId,
        Long rank,
        Integer retryAfter,
        String message,
        @JsonIgnore
        String token
) {
}
