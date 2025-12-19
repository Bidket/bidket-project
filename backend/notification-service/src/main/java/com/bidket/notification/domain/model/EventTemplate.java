package com.bidket.notification.domain.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/*
 * Kafka 이벤트 공통 템플릿
 */
@Builder
public record EventTemplate(
        UUID eventId,              // 이벤트 고유 ID (멱등성 키)
        String eventType,          // 이벤트 의미 (near_turn, admitted, outbid, ...)
        LocalDateTime occurredAt,  // 이벤트 발생 시각 (ISO-8601 형식)
        String source,             // 이벤트 발생 서비스
        UUID userId,               // 대상 사용자
        Map<String, Object> data   // 이벤트별 사실 데이터
) {}

