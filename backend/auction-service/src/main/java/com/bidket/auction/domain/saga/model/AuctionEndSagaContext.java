package com.bidket.auction.domain.saga.model;

import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 경매 종료 Saga의 실행 컨텍스트
 * Saga 실행 상태와 진행 상황을 추적하는 Entity
 *
 * BACKLOG.md SAGA-001 (line 707-762) 참조
 */
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

    /**
     * 경매 ID
     */
    @Column(nullable = false, name = "auction_id")
    private UUID auctionId;

    /**
     * 낙찰자 ID
     */
    @Column(nullable = false, name = "winner_id")
    private UUID winnerId;

    /**
     * 낙찰 입찰 ID
     */
    @Column(nullable = false, name = "winning_bid_id")
    private UUID winningBidId;

    /**
     * 상품 사이즈 ID
     */
    @Column(nullable = false, name = "product_size_id")
    private UUID productSizeId;

    /**
     * 낙찰가
     */
    @Column(nullable = false, name = "final_price")
    private Long finalPrice;

    /**
     * 생성된 주문 ID (Step 1 성공 후 저장)
     */
    @Column(name = "order_id")
    private UUID orderId;

    /**
     * Saga 현재 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SagaStatus status;

    /**
     * Saga 현재 단계
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, name = "current_step")
    private SagaStep currentStep;

    /**
     * 실패 사유 (실패 시)
     */
    @Column(length = 500, name = "failure_reason")
    private String failureReason;

    /**
     * 재시도 횟수
     */
    @Column(nullable = false, name = "retry_count")
    private Integer retryCount;

    /**
     * correlationId (이벤트 추적용)
     */
    @Column(name = "correlation_id")
    private UUID correlationId;

    /**
     * 완료된 단계 (SAGA-005: 정확히 한 번 보장)
     * 비트마스크로 저장: 각 비트가 SagaStep의 ordinal에 해당
     */
    @Column(name = "completed_steps", nullable = false)
    private Integer completedSteps = 0;

    @Version
    private Long version;

    // ===== Private 생성자 =====
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

    // ===== Builder =====
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

    // ===== 비즈니스 메서드 =====

    /**
     * Saga 시작
     */
    public void start() {
        if (this.status != SagaStatus.PENDING) {
            throw new IllegalStateException("Saga can only be started from PENDING status");
        }
        this.status = SagaStatus.IN_PROGRESS;
    }

    /**
     * 다음 단계로 진행
     */
    public void proceedToNextStep() {
        SagaStep nextStep = this.currentStep.next();
        if (nextStep == null) {
            throw new IllegalStateException("No next step available for " + this.currentStep);
        }
        this.currentStep = nextStep;
    }

    /**
     * 주문 ID 저장 (CREATE_ORDER 성공 시)
     */
    public void recordOrderId(UUID orderId) {
        if (this.currentStep != SagaStep.CREATE_ORDER) {
            throw new IllegalStateException("Can only record order ID during CREATE_ORDER step");
        }
        this.orderId = orderId;
    }

    /**
     * Saga 완료 처리
     */
    public void complete() {
        if (this.status != SagaStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete IN_PROGRESS saga");
        }
        this.status = SagaStatus.COMPLETED;
    }

    /**
     * 보상 트랜잭션 시작
     */
    public void startCompensation(String reason) {
        if (this.status != SagaStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only compensate IN_PROGRESS saga");
        }
        this.status = SagaStatus.COMPENSATING;
        this.failureReason = reason;
    }

    /**
     * 보상 트랜잭션 완료
     */
    public void completeCompensation() {
        if (this.status != SagaStatus.COMPENSATING) {
            throw new IllegalStateException("Can only complete COMPENSATING saga");
        }
        this.status = SagaStatus.COMPENSATED;
    }

    /**
     * Saga 실패 처리
     */
    public void fail(String reason) {
        this.status = SagaStatus.FAILED;
        this.failureReason = reason;
    }

    /**
     * 재시도 횟수 증가
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * SAGA-005: 정확히 한 번 보장 - 단계 완료 여부 확인
     *
     * @param step 확인할 단계
     * @return 이미 완료된 단계인 경우 true
     */
    public boolean isStepCompleted(SagaStep step) {
        int stepBit = 1 << step.ordinal();
        return (this.completedSteps & stepBit) != 0;
    }

    /**
     * SAGA-005: 정확히 한 번 보장 - 단계 완료 표시
     *
     * @param step 완료된 단계
     */
    public void markStepCompleted(SagaStep step) {
        int stepBit = 1 << step.ordinal();
        this.completedSteps |= stepBit;
    }

    /**
     * SAGA-005: Idempotency Key 생성
     *
     * @param step Saga 단계
     * @return Idempotency Key (sagaId:stepName 형식)
     */
    public String getIdempotencyKey(SagaStep step) {
        return String.format("%s:%s", this.id, step.name());
    }
}
