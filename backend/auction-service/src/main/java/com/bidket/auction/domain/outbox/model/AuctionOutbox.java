package com.bidket.auction.domain.outbox.model;

import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "auction_outbox", indexes = {
        @Index(name = "idx_auction_outbox_status_created", columnList = "status, created_at"),
        @Index(name = "idx_auction_outbox_aggregate", columnList = "aggregate_type, aggregate_id"),
        @Index(name = "idx_auction_outbox_correlation", columnList = "correlation_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionOutbox extends BaseEntity {

    private static final long[] BACKOFF_MINUTES = {0, 1, 4, 16};

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

    private AuctionOutbox(String aggregateType,
                          UUID aggregateId,
                          String eventType,
                          UUID correlationId,
                          String payload,
                          OutboxStatus status,
                          int retryCount,
                          String errorMessage,
                          LocalDateTime publishedAt,
                          Long version) {
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.status = status != null ? status : OutboxStatus.PENDING;
        this.retryCount = retryCount;
        this.errorMessage = errorMessage;
        this.publishedAt = publishedAt;
        this.version = version;
    }

    public static AuctionOutbox pending(String aggregateType,
                                        UUID aggregateId,
                                        String eventType,
                                        String payload,
                                        UUID correlationId) {
        return new AuctionOutbox(
                aggregateType,
                aggregateId,
                eventType,
                correlationId,
                payload,
                OutboxStatus.PENDING,
                0,
                null,
                null,
                null
        );
    }

    public boolean canPublish(int maxRetries, LocalDateTime now, Clock clock) {
        if (status == OutboxStatus.PUBLISHED || status == OutboxStatus.PUBLISHING) {
            return false;
        }
        if (retryCount >= maxRetries) {
            return false;
        }
        if (status == OutboxStatus.PENDING) {
            return true;
        }

        LocalDateTime lastUpdated = getUpdatedAt() != null
                ? getUpdatedAt()
                : Objects.requireNonNullElseGet(getCreatedAt(), () -> LocalDateTime.now(clock));

        LocalDateTime nextRetryAt = lastUpdated.plusMinutes(backoffMinutesForRetry(retryCount));
        return !nextRetryAt.isAfter(now);
    }

    public void markPublishing() {
        this.status = OutboxStatus.PUBLISHING;
    }

    public void markPublished(LocalDateTime publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage, int maxRetries) {
        this.status = OutboxStatus.FAILED;
        this.retryCount = Math.min(this.retryCount + 1, maxRetries);
        this.publishedAt = null;
        this.errorMessage = truncateErrorMessage(errorMessage);
    }

    private long backoffMinutesForRetry(int retryCount) {
        int index = Math.min(retryCount, BACKOFF_MINUTES.length - 1);
        return BACKOFF_MINUTES[index];
    }

    private String truncateErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
