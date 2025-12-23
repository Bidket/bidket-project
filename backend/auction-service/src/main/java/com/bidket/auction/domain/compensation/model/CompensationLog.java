package com.bidket.auction.domain.compensation.model;

import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "compensation_log", indexes = {
        @Index(name = "idx_compensation_saga_id", columnList = "saga_id"),
        @Index(name = "idx_compensation_saga_type", columnList = "saga_type"),
        @Index(name = "idx_compensation_aggregate", columnList = "aggregate_type, aggregate_id"),
        @Index(name = "idx_compensation_status", columnList = "status, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompensationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "saga_id")
    private UUID sagaId;

    @Column(nullable = false, length = 50, name = "saga_type")
    private String sagaType;

    @Column(nullable = false, length = 50, name = "aggregate_type")
    private String aggregateType;

    @Column(nullable = false, name = "aggregate_id")
    private UUID aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, name = "compensation_type")
    private CompensationType compensationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompensationStatus status;

    @Column(nullable = false, name = "step_number")
    private Integer stepNumber;

    @Column(nullable = false, name = "retry_count")
    private Integer retryCount;

    @Column(nullable = false, name = "max_retries")
    private Integer maxRetries;

    @Column(columnDefinition = "TEXT", name = "error_message")
    private String errorMessage;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private CompensationLog(UUID id, UUID sagaId, String sagaType, String aggregateType,
                           UUID aggregateId, CompensationType compensationType,
                           CompensationStatus status, Integer stepNumber, Integer retryCount,
                           Integer maxRetries, String errorMessage, String payload,
                           LocalDateTime executedAt, LocalDateTime completedAt) {
        this.id = id;
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.compensationType = compensationType;
        this.status = status;
        this.stepNumber = stepNumber;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.errorMessage = errorMessage;
        this.payload = payload;
        this.executedAt = executedAt;
        this.completedAt = completedAt;
    }

    public static CompensationLogBuilder builder() {
        return new CompensationLogBuilder();
    }

    public static class CompensationLogBuilder {
        private UUID id;
        private UUID sagaId;
        private String sagaType;
        private String aggregateType;
        private UUID aggregateId;
        private CompensationType compensationType;
        private CompensationStatus status;
        private Integer stepNumber;
        private Integer retryCount;
        private Integer maxRetries;
        private String errorMessage;
        private String payload;
        private LocalDateTime executedAt;
        private LocalDateTime completedAt;

        public CompensationLogBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public CompensationLogBuilder sagaId(UUID sagaId) {
            this.sagaId = sagaId;
            return this;
        }

        public CompensationLogBuilder sagaType(String sagaType) {
            this.sagaType = sagaType;
            return this;
        }

        public CompensationLogBuilder aggregateType(String aggregateType) {
            this.aggregateType = aggregateType;
            return this;
        }

        public CompensationLogBuilder aggregateId(UUID aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public CompensationLogBuilder compensationType(CompensationType compensationType) {
            this.compensationType = compensationType;
            return this;
        }

        public CompensationLogBuilder status(CompensationStatus status) {
            this.status = status;
            return this;
        }

        public CompensationLogBuilder stepNumber(Integer stepNumber) {
            this.stepNumber = stepNumber;
            return this;
        }

        public CompensationLogBuilder retryCount(Integer retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public CompensationLogBuilder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public CompensationLogBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public CompensationLogBuilder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public CompensationLogBuilder executedAt(LocalDateTime executedAt) {
            this.executedAt = executedAt;
            return this;
        }

        public CompensationLogBuilder completedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public CompensationLog build() {
             
            if (status == null) {
                status = CompensationStatus.PENDING;
            }
            if (retryCount == null) {
                retryCount = 0;
            }
            if (maxRetries == null) {
                maxRetries = 3;
            }

            return new CompensationLog(
                    id, sagaId, sagaType, aggregateType, aggregateId,
                    compensationType, status, stepNumber, retryCount,
                    maxRetries, errorMessage, payload, executedAt, completedAt
            );
        }
    }

    public void startExecution() {
        if (this.status != CompensationStatus.PENDING && this.status != CompensationStatus.FAILED) {
            throw new IllegalStateException(
                    String.format("Cannot start execution from status: %s", this.status)
            );
        }
        this.status = CompensationStatus.IN_PROGRESS;
        this.executedAt = LocalDateTime.now();
    }

    public void complete() {
        if (this.status != CompensationStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    String.format("Cannot complete from status: %s", this.status)
            );
        }
        this.status = CompensationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = null;  
    }

    public void fail(String errorMessage) {
        this.status = CompensationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public boolean incrementRetryCount() {
        this.retryCount++;
        return this.retryCount < this.maxRetries;
    }

    public boolean canRetry() {
        return this.status.isRetryable() && this.retryCount < this.maxRetries;
    }

    public boolean isMaxRetriesExceeded() {
        return this.retryCount >= this.maxRetries;
    }
}
