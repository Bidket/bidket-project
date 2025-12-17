package com.bidket.auction.domain.processed.model;

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

@Getter
@Entity
@Table(name = "processed_events", indexes = {
        @Index(name = "idx_processed_events_correlation", columnList = "correlation_id"),
        @Index(name = "idx_processed_events_processed_at", columnList = "processed_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent extends BaseEntity {

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

    private ProcessedEvent(UUID eventId, String eventType, UUID correlationId, LocalDateTime processedAt, String result) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.correlationId = correlationId;
        this.processedAt = processedAt;
        this.result = result;
    }

    public static ProcessedEvent of(UUID eventId, String eventType, UUID correlationId, String result) {
        return new ProcessedEvent(eventId, eventType, correlationId, LocalDateTime.now(), result);
    }
}
