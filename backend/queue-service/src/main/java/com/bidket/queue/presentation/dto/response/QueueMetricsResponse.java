package com.bidket.queue.presentation.dto.response;

import com.bidket.queue.domain.model.QueueConfigStatus;
import com.bidket.queue.domain.model.QueueMetrics;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record QueueMetricsResponse(
        UUID auctionId,
        QueueConfigStatus status,
        QueueMetrics metrics,
        LocalDateTime lastUpdated,
        UUID lastUpdatedBy
) {
}
