package com.bidket.queue.presentation.dto.response;

import com.bidket.queue.domain.model.QueueStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record QueueStatusResponse(
        UUID auctionId,
        Long totalWaiting,
        Long currentActive,
        QueueStatus status
) {
}
