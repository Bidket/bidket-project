package com.bidket.queue.domain.model;

import lombok.Builder;

@Builder
public record QueueMetrics(
        Long totalWaiting,
        Long currentActive,
        Long totalTraffic,
        Long maxActive
) {
}
