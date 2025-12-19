package com.bidket.queue.domain.model.outbox;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record QueueOutboxModel(
        String id,
        UUID aggregateId,
        String aggregateType,
        String topic,
        String key,
        String payload,
        EventType eventType,
        UUID correlationId,
        int retryCount,
        String errorMessage,
        LocalDateTime publishedAt,
        OutboxStatus status
) {
}
