package com.bidket.queue.domain.event;

import java.util.UUID;

public record QueueNearTurnEvent(
        UUID auctionId,
        UUID userId,
        int position
) {
}
