package com.bidket.queue.presentation.dto.response;

import com.bidket.queue.domain.model.QueueTrafficStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record QueueStatusResponse(
        UUID auctionId,
        Long totalWaiting,
        Long currentActive,
        QueueTrafficStatus status
) {
}
