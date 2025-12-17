package com.bidket.order.infrastructure.processed.entity;

import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 처리된 이벤트 엔티티
 * 멱등성 보장을 위한 이벤트 추적 테이블
 */
@Getter
@Entity
@Table(name = "processed_events", indexes = {
        @Index(name = "idx_processed_events_correlation", columnList = "correlation_id"),
        @Index(name = "idx_processed_events_processed_at", columnList = "processed_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEventEntity extends BaseEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "jsonb")
    private String result;

    private ProcessedEventEntity(UUID eventId, String eventType, UUID correlationId, LocalDateTime processedAt, String result) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.correlationId = correlationId;
        this.processedAt = processedAt;
        this.result = result;
    }

    public static ProcessedEventEntity create(UUID eventId, String eventType, UUID correlationId, LocalDateTime processedAt, String result) {
        return new ProcessedEventEntity(eventId, eventType, correlationId, processedAt, result);
    }
}
