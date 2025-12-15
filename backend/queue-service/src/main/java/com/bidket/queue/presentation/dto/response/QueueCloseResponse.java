package com.bidket.queue.presentation.dto.response;

import com.bidket.queue.domain.model.QueueConfigStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record QueueCloseResponse(
        UUID auctionId,
        QueueConfigStatus status,
        Long flushedWaitingUser,
        Long flushedActiveUser,
        LocalDateTime closedAt
) {
}
