package com.bidket.auction.infrastructure.kafka.event;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public record StandardEvent(
        UUID eventId,
        String eventType,
        LocalDateTime occurredAt,
        String source,
        UUID userId,
        Map<String, Object> data
) {
    public StandardEvent {
        if (eventId == null) throw new IllegalArgumentException("eventId must not be null");
        if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("eventType must not be blank");
        if (occurredAt == null) throw new IllegalArgumentException("occurredAt must not be null");
        if (source == null || source.isBlank()) throw new IllegalArgumentException("source must not be blank");
        if (data == null) throw new IllegalArgumentException("data must not be null");
    }

    public static StandardEvent of(String source, String eventType, UUID userId, Map<String, Object> data) {
        return StandardEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .occurredAt(LocalDateTime.now())
                .source(source)
                .userId(userId)
                .data(data)
                .build();
    }
}
