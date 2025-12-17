package com.bidket.order.domain.processed.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 처리된 이벤트 도메인 모델
 * 멱등성(Idempotency) 보장을 위해 이벤트 ID를 추적합니다.
 */
public record ProcessedEvent(
        UUID eventId,
        String eventType,
        UUID correlationId,
        LocalDateTime processedAt,
        String result
) {
    public ProcessedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId는 필수입니다");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType은 필수입니다");
        }
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }

    public static ProcessedEvent of(UUID eventId, String eventType, UUID correlationId, String result) {
        return new ProcessedEvent(eventId, eventType, correlationId, LocalDateTime.now(), result);
    }
}
