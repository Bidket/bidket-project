package com.bidket.queue.presentation.dto.response;

import com.bidket.queue.domain.model.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.util.UUID;

@Builder
public record QueueAccommodatableResponse(
        UUID auctionId,
        UUID userId,
        UserStatus status,
        Long rank,
        Integer retryAfter,
        @JsonIgnore
        String token,
        String message
) {
}
