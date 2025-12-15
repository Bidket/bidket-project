package com.bidket.queue.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationEvent(
        UUID auctionId,
        UUID userId,
        LocalDateTime enterTime
) {
}
