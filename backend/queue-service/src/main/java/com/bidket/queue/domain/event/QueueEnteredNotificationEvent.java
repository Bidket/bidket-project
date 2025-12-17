package com.bidket.queue.domain.event;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record QueueEnteredNotificationEvent(
        UUID auctionId,
        UUID userId,
        LocalDateTime enterTime
) {
}
