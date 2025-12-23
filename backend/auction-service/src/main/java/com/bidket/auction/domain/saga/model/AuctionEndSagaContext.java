package com.bidket.auction.domain.saga.model;

import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "auction_end_saga_context", indexes = {
        @Index(name = "idx_saga_auction", columnList = "auction_id"),
        @Index(name = "idx_saga_status", columnList = "status"),
        @Index(name = "idx_saga_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionEndSagaContext extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "auction_id")
    private UUID auctionId;

    @Column(nullable = false, name = "winner_id")
    private UUID winnerId;

    @Column(nullable = false, name = "winning_bid_id")
    private UUID winningBidId;

    @Column(nullable = false, name = "product_size_id")
    private UUID productSizeId;

    @Column(nullable = false, name = "final_price")
    private Long finalPrice;

    @Column(name = "order_id")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SagaStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, name = "current_step")
    private SagaStep currentStep;

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

    private AuctionEndSagaContext(UUID id, UUID auctionId, UUID winnerId, UUID winningBidId,
                                  UUID productSizeId, Long finalPrice, UUID orderId,
                                  SagaStatus status, SagaStep currentStep, String failureReason,
                                  Integer retryCount, UUID correlationId, Integer completedSteps, Long version) {
        this.id = id;
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.winningBidId = winningBidId;
        this.productSizeId = productSizeId;
        this.finalPrice = finalPrice;
        this.orderId = orderId;
        this.status = status;
        this.currentStep = currentStep;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
        this.correlationId = correlationId;
        this.completedSteps = completedSteps != null ? completedSteps : 0;
        this.version = version;
    }

    public static AuctionEndSagaContextBuilder builder() {
        return new AuctionEndSagaContextBuilder();
    }

    public static class AuctionEndSagaContextBuilder {
        private UUID id;
        private UUID auctionId;
        private UUID winnerId;
        private UUID winningBidId;
        private UUID productSizeId;
        private Long finalPrice;
        private UUID orderId;
        private SagaStatus status;
        private SagaStep currentStep;
        private String failureReason;
        private Integer retryCount;
        private UUID correlationId;
        private Integer completedSteps;
        private Long version;

        public AuctionEndSagaContextBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public AuctionEndSagaContextBuilder auctionId(UUID auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public AuctionEndSagaContextBuilder winnerId(UUID winnerId) {
            this.winnerId = winnerId;
            return this;
        }

        public AuctionEndSagaContextBuilder winningBidId(UUID winningBidId) {
            this.winningBidId = winningBidId;
            return this;
        }

        public AuctionEndSagaContextBuilder productSizeId(UUID productSizeId) {
            this.productSizeId = productSizeId;
            return this;
        }

        public AuctionEndSagaContextBuilder finalPrice(Long finalPrice) {
            this.finalPrice = finalPrice;
            return this;
        }

        public AuctionEndSagaContextBuilder orderId(UUID orderId) {
            this.orderId = orderId;
            return this;
        }

        public AuctionEndSagaContextBuilder status(SagaStatus status) {
            this.status = status;
            return this;
        }

        public AuctionEndSagaContextBuilder currentStep(SagaStep currentStep) {
            this.currentStep = currentStep;
            return this;
        }

        public AuctionEndSagaContextBuilder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public AuctionEndSagaContextBuilder retryCount(Integer retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public AuctionEndSagaContextBuilder correlationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public AuctionEndSagaContextBuilder completedSteps(Integer completedSteps) {
            this.completedSteps = completedSteps;
            return this;
        }

        public AuctionEndSagaContextBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public AuctionEndSagaContext build() {
            if (status == null) {
                status = SagaStatus.PENDING;
            }
            if (currentStep == null) {
                currentStep = SagaStep.CREATE_ORDER;
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

            return new AuctionEndSagaContext(
                    id, auctionId, winnerId, winningBidId,
                    productSizeId, finalPrice, orderId,
                    status, currentStep, failureReason,
                    retryCount, correlationId, completedSteps, version
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
        SagaStep nextStep = this.currentStep.next();
        if (nextStep == null) {
            throw new IllegalStateException("No next step available for " + this.currentStep);
        }
        this.currentStep = nextStep;
    }

    public void recordOrderId(UUID orderId) {
        if (this.currentStep != SagaStep.CREATE_ORDER) {
            throw new IllegalStateException("Can only record order ID during CREATE_ORDER step");
        }
        this.orderId = orderId;
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

    public boolean isStepCompleted(SagaStep step) {
        int stepBit = 1 << step.ordinal();
        return (this.completedSteps & stepBit) != 0;
    }

    public void markStepCompleted(SagaStep step) {
        int stepBit = 1 << step.ordinal();
        this.completedSteps |= stepBit;
    }

    public String getIdempotencyKey(SagaStep step) {
        return String.format("%s:%s", this.id, step.name());
    }
}
