package com.bidket.queue.domain.event;

import lombok.Builder;

import java.util.UUID;

@Builder
public record QueueNearTurnEvent(
        UUID auctionId,
        UUID userId,
        long rank
) {
}
