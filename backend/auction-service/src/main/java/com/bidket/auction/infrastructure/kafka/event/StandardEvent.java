package com.bidket.auction.infrastructure.kafka.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Bidket 표준 이벤트 템플릿
 *
 * 모든 이벤트는 이 구조를 따릅니다.
 * - eventId: 멱등성(Idempotency) 체크에 필수
 * - occurredAt: 이벤트 발생 시각
 * - source: 이벤트를 생산한 서비스 (예: "auction-service", "order-service")
 * - type: 이벤트 타입 (예: "CREATE_ORDER_REQUESTED", "ORDER_CREATED")
 * - userId: 이벤트와 연관된 사용자 ID (추적 및 권한 검증용)
 * - data: 처리에 필수적인 최소 정보만 포함 (엔티티 스냅샷 X)
 */
public record StandardEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        String source,
        String type,
        UUID userId,
        Map<String, Object> data
) {
    public StandardEvent {
        if (eventId == null) throw new IllegalArgumentException("eventId must not be null");
        if (occurredAt == null) throw new IllegalArgumentException("occurredAt must not be null");
        if (source == null || source.isBlank()) throw new IllegalArgumentException("source must not be blank");
        if (type == null || type.isBlank()) throw new IllegalArgumentException("type must not be blank");
        if (data == null) throw new IllegalArgumentException("data must not be null");
    }

    public static StandardEvent of(String source, String type, UUID userId, Map<String, Object> data) {
        return new StandardEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                source,
                type,
                userId,
                data
        );
    }
}
