package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record StandardEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        String source,
        String eventType,
        Map<String, Object> data
) {
    public StandardEvent {
        if (eventId == null) throw new IllegalArgumentException("eventId must not be null");
        if (occurredAt == null) throw new IllegalArgumentException("occurredAt must not be null");
        if (source == null || source.isBlank()) throw new IllegalArgumentException("source must not be blank");
        if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("eventType must not be blank");
        if (data == null) throw new IllegalArgumentException("data must not be null");
    }

    public static StandardEvent of(String source, String eventType, Map<String, Object> data) {
        return new StandardEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                source,
                eventType,
                data
        );
    }
}
