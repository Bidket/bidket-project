package com.bidket.order.infrastructure.kafka.event;

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
 * - eventType: 이벤트 타입 (예: "CREATE_ORDER_REQUESTED", "ORDER_CREATED")
 * - userId: 이벤트와 연관된 사용자 ID (추적 및 권한 검증용)
 * - data: 처리에 필수적인 최소 정보만 포함 (엔티티 스냅샷 X)
 */
public record StandardEvent(
        UUID eventId,
        LocalDateTime occurredAt,
        String source,
        String eventType,
        UUID userId,
        Map<String, Object> data
) {
    public StandardEvent {
        if (eventId == null) throw new IllegalArgumentException("eventId는 필수입니다");
        if (occurredAt == null) throw new IllegalArgumentException("occurredAt는 필수입니다");
        if (source == null || source.isBlank()) throw new IllegalArgumentException("source는 필수입니다");
        if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("eventType은 필수입니다");
        if (data == null) throw new IllegalArgumentException("data는 필수입니다");
    }

    public static StandardEvent of(String source, String eventType, UUID userId, Map<String, Object> data) {
        return new StandardEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                source,
                eventType,
                userId,
                data
        );
    }
}
