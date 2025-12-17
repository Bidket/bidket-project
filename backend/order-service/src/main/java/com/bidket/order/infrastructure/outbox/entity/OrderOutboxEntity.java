package com.bidket.order.infrastructure.outbox.entity;

import com.bidket.common.infra.BaseEntity;
import com.bidket.order.domain.outbox.model.OutboxStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OrderOutbox JPA 엔티티
 *
 * Transactional Outbox Pattern 구현
 * - 이벤트를 DB 테이블에 저장 (트랜잭션 안전성 보장)
 * - 별도 Publisher가 폴링하여 Kafka로 발행
 */
@Entity
@Table(name = "order_outbox", indexes = {
        @Index(name = "idx_order_outbox_status_created", columnList = "status, created_at"),
        @Index(name = "idx_order_outbox_aggregate", columnList = "aggregate_type, aggregate_id"),
        @Index(name = "idx_order_outbox_correlation", columnList = "correlation_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderOutboxEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Version
    private Long version;

    public OrderOutboxEntity(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            UUID correlationId,
            String payload,
            OutboxStatus status,
            int retryCount,
            String errorMessage,
            LocalDateTime publishedAt,
            Long version
    ) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.correlationId = correlationId;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
        this.errorMessage = errorMessage;
        this.publishedAt = publishedAt;
        this.version = version;
    }

    public void updateStatus(OutboxStatus newStatus) {
        this.status = newStatus;
    }

    public void updatePublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void updateErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
