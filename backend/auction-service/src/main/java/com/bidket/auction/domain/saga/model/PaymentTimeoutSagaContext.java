package com.bidket.auction.domain.saga.model;

import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "payment_timeout_saga_context", indexes = {
        @Index(name = "idx_timeout_saga_auction", columnList = "auction_id"),
        @Index(name = "idx_timeout_saga_order", columnList = "order_id"),
        @Index(name = "idx_timeout_saga_status", columnList = "status"),
        @Index(name = "idx_timeout_saga_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTimeoutSagaContext extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "auction_id")
    private UUID auctionId;

    @Column(nullable = false, name = "order_id")
    private UUID orderId;

    @Column(nullable = false, name = "winner_id")
    private UUID winnerId;

    @Column(nullable = false, name = "winning_bid_id")
    private UUID winningBidId;

    @Column(nullable = false, name = "product_size_id")
    private UUID productSizeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SagaStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, name = "current_step")
    private PaymentTimeoutSagaStep currentStep;

    @Column(length = 500, name = "failure_reason")
    private String failureReason;

    @Column(nullable = false, name = "retry_count")
    private Integer retryCount;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "completed_steps", nullable = false)
    private Integer completedSteps = 0;

    @Version
    private Long version;

    private PaymentTimeoutSagaContext(UUID id, UUID auctionId, UUID orderId, UUID winnerId,
                                      UUID winningBidId, UUID productSizeId,
                                      SagaStatus status, PaymentTimeoutSagaStep currentStep,
                                      String failureReason, Integer retryCount,
                                      UUID correlationId, Integer completedSteps, Long version) {
        this.id = id;
        this.auctionId = auctionId;
        this.orderId = orderId;
        this.winnerId = winnerId;
        this.winningBidId = winningBidId;
        this.productSizeId = productSizeId;
        this.status = status;
        this.currentStep = currentStep;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
        this.correlationId = correlationId;
        this.completedSteps = completedSteps != null ? completedSteps : 0;
        this.version = version;
    }

    public static PaymentTimeoutSagaContextBuilder builder() {
        return new PaymentTimeoutSagaContextBuilder();
    }

    public static class PaymentTimeoutSagaContextBuilder {
        private UUID id;
        private UUID auctionId;
        private UUID orderId;
        private UUID winnerId;
        private UUID winningBidId;
        private UUID productSizeId;
        private SagaStatus status;
        private PaymentTimeoutSagaStep currentStep;
        private String failureReason;
        private Integer retryCount;
        private UUID correlationId;
        private Integer completedSteps;
        private Long version;

        public PaymentTimeoutSagaContextBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public PaymentTimeoutSagaContextBuilder auctionId(UUID auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public PaymentTimeoutSagaContextBuilder orderId(UUID orderId) {
            this.orderId = orderId;
            return this;
        }

        public PaymentTimeoutSagaContextBuilder winnerId(UUID winnerId) {
            this.winnerId = winnerId;
            return this;
        }

        public PaymentTimeoutSagaContextBuilder winningBidId(UUID winningBidId) {
            this.winningBidId = winningBidId;
            return this;
        }

        public PaymentTimeoutSagaContextBuilder productSizeId(UUID productSizeId) {
            this.productSizeId = productSizeId;
            return this;
        }

        public PaymentTimeoutSagaContextBuilder status(SagaStatus status) {
            this.status = status;
            return this;
        }

        public PaymentTimeoutSagaContextBuilder currentStep(PaymentTimeoutSagaStep currentStep) {
            this.currentStep = currentStep;
            return this;
        }

        public PaymentTimeoutSagaContextBuilder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public PaymentTimeoutSagaContextBuilder retryCount(Integer retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public PaymentTimeoutSagaContextBuilder correlationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public PaymentTimeoutSagaContextBuilder completedSteps(Integer completedSteps) {
            this.completedSteps = completedSteps;
            return this;
        }

        public PaymentTimeoutSagaContextBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public PaymentTimeoutSagaContext build() {
            if (status == null) {
                status = SagaStatus.PENDING;
            }
            if (currentStep == null) {
                currentStep = PaymentTimeoutSagaStep.REOPEN_AUCTION;
            }
            if (retryCount == null) {
                retryCount = 0;
            }
            if (correlationId == null) {
                correlationId = UUID.randomUUID();
            }
            if (completedSteps == null) {
                completedSteps = 0;
            }

            return new PaymentTimeoutSagaContext(
                    id, auctionId, orderId, winnerId, winningBidId, productSizeId,
                    status, currentStep, failureReason, retryCount,
                    correlationId, completedSteps, version
            );
        }
    }

    public void start() {
        if (this.status != SagaStatus.PENDING) {
            throw new IllegalStateException("Saga can only be started from PENDING status");
        }
        this.status = SagaStatus.IN_PROGRESS;
    }

    public void proceedToNextStep() {
        PaymentTimeoutSagaStep nextStep = this.currentStep.next();
        if (nextStep == null) {
            throw new IllegalStateException("No next step available for " + this.currentStep);
        }
        this.currentStep = nextStep;
    }

    public void complete() {
        if (this.status != SagaStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete IN_PROGRESS saga");
        }
        this.status = SagaStatus.COMPLETED;
    }

    public void startCompensation(String reason) {
        if (this.status != SagaStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only compensate IN_PROGRESS saga");
        }
        this.status = SagaStatus.COMPENSATING;
        this.failureReason = reason;
    }

    public void completeCompensation() {
        if (this.status != SagaStatus.COMPENSATING) {
            throw new IllegalStateException("Can only complete COMPENSATING saga");
        }
        this.status = SagaStatus.COMPENSATED;
    }

    public void fail(String reason) {
        this.status = SagaStatus.FAILED;
        this.failureReason = reason;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public boolean isStepCompleted(PaymentTimeoutSagaStep step) {
        int stepBit = 1 << step.ordinal();
        return (this.completedSteps & stepBit) != 0;
    }

    public void markStepCompleted(PaymentTimeoutSagaStep step) {
        int stepBit = 1 << step.ordinal();
        this.completedSteps |= stepBit;
    }

    public String getIdempotencyKey(PaymentTimeoutSagaStep step) {
        return String.format("%s:%s", this.id, step.name());
    }
}
