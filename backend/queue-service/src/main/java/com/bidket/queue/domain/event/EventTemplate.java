package com.bidket.queue.domain.event;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public record EventTemplate(
        UUID eventId,
        LocalDateTime occurredAt,
        String source,
        UUID userId,
        String eventType,
        Map<String, Object> data
) {
    public static EventTemplate of(UUID userId, String source, String eventType, Map<String, Object> data) {
        return EventTemplate.builder()
                .eventId(UUID.randomUUID())
                .occurredAt(LocalDateTime.now())
                .userId(userId)
                .source(source)
                .eventType(eventType)
                .data(data)
                .build();
    }
}
