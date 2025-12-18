package com.bidket.auction.application.saga;

import com.bidket.auction.application.compensation.CompensationExecutor;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.repository.BidRepository;
import com.bidket.auction.domain.compensation.model.CompensationType;
import com.bidket.auction.domain.saga.model.AuctionEndSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;
import com.bidket.auction.domain.saga.model.SagaStep;
import com.bidket.auction.domain.saga.repository.AuctionEndSagaContextRepository;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import com.bidket.auction.infrastructure.kafka.event.CreateOrderRequestedEvent;
import com.bidket.auction.infrastructure.notification.NotificationEventProducer;
import com.bidket.auction.infrastructure.order.OrderEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * 경매 종료 Saga Orchestrator
 * BACKLOG.md SAGA-001 (line 707-762) 구현
 *
 * Saga Flow:
 * 1. CREATE_ORDER: Order Service에 주문 생성 요청
 * 2. MARK_WINNING_BID: 낙찰 입찰 상태를 WON으로 변경
 * 3. FINALIZE_AUCTION: 경매 상태를 SUCCESS로 변경
 * 4. PUBLISH_END_EVENT: AUCTION_ENDED 이벤트 발행
 *
 * Compensation:
 * - ORDER_CREATION_FAILED 시 경매 재오픈 (ACTIVE, endTime + 24h)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionEndSagaOrchestrator {

    private final AuctionEndSagaContextRepository sagaRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final OrderEventProducer orderEventProducer;
    private final NotificationEventProducer notificationEventProducer;
    private final CompensationExecutor compensationExecutor;
    private final ObjectMapper objectMapper;

    /**
     * 경매 종료 Saga 시작
     * Scheduler 또는 즉시 구매 API에서 호출
     *
     * SAGA-005: Bulkhead로 동시 실행 제한 (max 10)
     *
     * @param auctionId 종료할 경매 ID
     * @return 생성된 Saga ID
     */
    @Transactional
    @Bulkhead(name = "sagaOrchestrator", fallbackMethod = "startAuctionEndSagaFallback")
    public UUID startAuctionEndSaga(UUID auctionId) {
        log.info("[AuctionEndSaga] Saga 시작: auctionId={}", auctionId);

        // 1. 경매 조회 및 검증
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        // 2. 최고 입찰 조회
        Bid winningBid = bidRepository.findHighestBidByAuctionId(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.NO_BIDS_FOUND));

        // 3. Saga Context 생성
        AuctionEndSagaContext sagaContext = AuctionEndSagaContext.builder()
                .auctionId(auctionId)
                .winnerId(winningBid.getBidderId())
                .winningBidId(winningBid.getId())
                .productSizeId(auction.getProductSizeId())
                .finalPrice(winningBid.getAmount())
                .status(SagaStatus.PENDING)
                .currentStep(SagaStep.CREATE_ORDER)
                .build();

        sagaContext = sagaRepository.save(sagaContext);

        // 4. Saga 시작
        sagaContext.start();
        sagaContext = sagaRepository.save(sagaContext);

        try {
            // 5. Step 1: 주문 생성 요청
            executeCreateOrderStep(sagaContext);

            log.info("[AuctionEndSaga] Saga 시작 완료: sagaId={}, auctionId={}", sagaContext.getId(), auctionId);

        } catch (Exception e) {
            log.error("[AuctionEndSaga] Saga 실패: sagaId={}, error={}",
                    sagaContext.getId(), e.getMessage(), e);

            sagaContext.fail(e.getMessage());
            sagaRepository.save(sagaContext);

            // 보상 트랜잭션 실행
            compensate(sagaContext.getId(), "AuctionEndSagaFailed: " + e.getMessage());

            throw e;
        }

        return sagaContext.getId();
    }

    /**
     * Step 1: Order Service에 주문 생성 요청
     *
     * SAGA-005:
     * - CircuitBreaker: Order Service 장애 시 Circuit Open
     * - Retry: 일시적 장애 시 최대 3회 재시도 (1s, 2s, 4s backoff)
     */
    @Transactional
    @CircuitBreaker(name = "orderService", fallbackMethod = "executeCreateOrderStepFallback")
    @Retry(name = "sagaStep")
    public void executeCreateOrderStep(AuctionEndSagaContext sagaContext) {
        log.info("[AuctionEndSaga] Step 1 시작: CREATE_ORDER, sagaId={}", sagaContext.getId());

        if (sagaContext.getCurrentStep() != SagaStep.CREATE_ORDER) {
            log.warn("[AuctionEndSaga] 잘못된 단계: expected=CREATE_ORDER, actual={}", sagaContext.getCurrentStep());
            return;
        }

        // 주문 생성 요청 이벤트 발행 (OutBox 패턴 사용)
        orderEventProducer.publishCreateOrderRequest(
                sagaContext.getId(),
                sagaContext.getAuctionId(),
                sagaContext.getWinnerId(),
                sagaContext.getProductSizeId(),
                sagaContext.getFinalPrice(),
                sagaContext.getCorrelationId()
        );

        log.info("[AuctionEndSaga] Step 1 완료: CREATE_ORDER 이벤트 발행, sagaId={}, correlationId={}",
                sagaContext.getId(), sagaContext.getCorrelationId());
    }

    /**
     * Step 2: 낙찰 입찰 상태 업데이트 (WON)
     */
    @Transactional
    public void executeMarkWinningBidStep(AuctionEndSagaContext sagaContext) {
        log.info("[AuctionEndSaga] Step 2 시작: MARK_WINNING_BID, sagaId={}", sagaContext.getId());

        if (sagaContext.getCurrentStep() != SagaStep.MARK_WINNING_BID) {
            log.warn("[AuctionEndSaga] 잘못된 단계: expected=MARK_WINNING_BID, actual={}", sagaContext.getCurrentStep());
            return;
        }

        // 낙찰 입찰 상태 업데이트
        Bid winningBid = bidRepository.findById(sagaContext.getWinningBidId())
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.BID_NOT_FOUND));

        winningBid.markAsWon();
        bidRepository.save(winningBid);

        // 다음 단계로 진행
        sagaContext.proceedToNextStep();
        sagaRepository.save(sagaContext);

        log.info("[AuctionEndSaga] Step 2 완료: Bid status=WON, bidId={}, sagaId={}",
                winningBid.getId(), sagaContext.getId());

        // Step 3 실행
        executeFinalizeAuctionStep(sagaContext);
    }

    /**
     * Step 3: 경매 최종 상태 업데이트 (SUCCESS)
     */
    @Transactional
    public void executeFinalizeAuctionStep(AuctionEndSagaContext sagaContext) {
        log.info("[AuctionEndSaga] Step 3 시작: FINALIZE_AUCTION, sagaId={}", sagaContext.getId());

        if (sagaContext.getCurrentStep() != SagaStep.FINALIZE_AUCTION) {
            log.warn("[AuctionEndSaga] 잘못된 단계: expected=FINALIZE_AUCTION, actual={}", sagaContext.getCurrentStep());
            return;
        }

        // 경매 상태 업데이트
        Auction auction = auctionRepository.findById(sagaContext.getAuctionId())
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        auction.end(true);
        auction.setWinner(
                sagaContext.getWinnerId(),
                sagaContext.getWinningBidId(),
                sagaContext.getFinalPrice()
        );
        auctionRepository.save(auction);

        // 다음 단계로 진행
        sagaContext.proceedToNextStep();
        sagaRepository.save(sagaContext);

        log.info("[AuctionEndSaga] Step 3 완료: Auction status=SUCCESS, auctionId={}, sagaId={}",
                auction.getId(), sagaContext.getId());

        // Step 4 실행
        executePublishEndEventStep(sagaContext);
    }

    /**
     * Step 4: 낙찰자/패찰자 알림 이벤트 발행
     *
     * INT-005 (Notification Service 연동) 구현:
     * - 낙찰자: result="WON" 알림 발행
     * - 패찰자들: result="LOST" 알림 발행
     */
    @Transactional
    public void executePublishEndEventStep(AuctionEndSagaContext sagaContext) {
        log.info("[AuctionEndSaga] Step 4 시작: PUBLISH_END_EVENT, sagaId={}", sagaContext.getId());

        if (sagaContext.getCurrentStep() != SagaStep.PUBLISH_END_EVENT) {
            log.warn("[AuctionEndSaga] 잘못된 단계: expected=PUBLISH_END_EVENT, actual={}", sagaContext.getCurrentStep());
            return;
        }

        LocalDateTime closedAt = LocalDateTime.now();

        // 1. 낙찰자 알림 발행
        notificationEventProducer.publishWinnerNotification(
                sagaContext.getWinnerId(),
                sagaContext.getAuctionId(),
                sagaContext.getFinalPrice(),
                closedAt,
                sagaContext.getCorrelationId()
        );

        log.info("[AuctionEndSaga] 낙찰자 알림 발행 완료: winnerId={}, auctionId={}",
                sagaContext.getWinnerId(), sagaContext.getAuctionId());

        // 2. 패찰자 알림 발행 (낙찰자를 제외한 모든 입찰자)
        List<Bid> allBids = bidRepository.findByAuctionId(sagaContext.getAuctionId());
        int loserCount = 0;

        for (Bid bid : allBids) {
            // 낙찰자는 제외
            if (!bid.getBidderId().equals(sagaContext.getWinnerId())) {
                notificationEventProducer.publishLoserNotification(
                        bid.getBidderId(),
                        sagaContext.getAuctionId(),
                        sagaContext.getFinalPrice(),
                        closedAt,
                        sagaContext.getCorrelationId()
                );
                loserCount++;
            }
        }

        log.info("[AuctionEndSaga] 패찰자 알림 발행 완료: loserCount={}, auctionId={}",
                loserCount, sagaContext.getAuctionId());

        // Saga 완료
        sagaContext.complete();
        sagaRepository.save(sagaContext);

        log.info("[AuctionEndSaga] Saga 완료: sagaId={}, auctionId={}, status=COMPLETED, totalNotifications={}",
                sagaContext.getId(), sagaContext.getAuctionId(), loserCount + 1);
    }

    /**
     * 보상 트랜잭션: 경매 재오픈
     * ORDER_CREATION_FAILED 이벤트 수신 시 호출
     *
     * BACKLOG.md SAGA-003 (line 581-583) 구현
     * - compensation_log 기반
     * - 재시도 3회
     * - 역순 보상 실행
     *
     * SAGA-005: Bulkhead로 동시 보상 실행 제한 (max 5)
     *
     * @param sagaId Saga ID
     * @param failureReason 실패 사유
     */
    @Transactional
    @Bulkhead(name = "sagaCompensation")
    public void compensate(UUID sagaId, String failureReason) {
        log.warn("[AuctionEndSaga] 보상 트랜잭션 시작: sagaId={}, reason={}", sagaId, failureReason);

        AuctionEndSagaContext sagaContext = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.SAGA_NOT_FOUND));

        // 보상 시작
        sagaContext.startCompensation(failureReason);
        sagaRepository.save(sagaContext);

        try {
            // 1. 보상 로그 생성 (역순 실행을 위해 step_number 사용)
            createCompensationLogs(sagaContext);

            // 2. CompensationExecutor를 통한 보상 실행 (역순 LIFO)
            compensationExecutor.executeCompensations(sagaId, failureReason);

            // 3. 보상 완료
            sagaContext.completeCompensation();
            sagaRepository.save(sagaContext);

            log.info("[AuctionEndSaga] 보상 트랜잭션 완료: sagaId={}, status=COMPENSATED",
                    sagaId);

        } catch (Exception e) {
            log.error("[AuctionEndSaga] 보상 트랜잭션 실패: sagaId={}, error={}",
                    sagaId, e.getMessage(), e);

            sagaContext.fail("보상 실패: " + e.getMessage());
            sagaRepository.save(sagaContext);

            throw new AuctionDomainException(AuctionErrorCode.SAGA_COMPENSATION_FAILED);
        }
    }

    /**
     * 보상 로그 생성
     * Saga의 각 단계에 대한 보상 로그를 역순으로 생성
     *
     * @param sagaContext Saga Context
     */
    private void createCompensationLogs(AuctionEndSagaContext sagaContext) {
        log.info("[AuctionEndSaga] 보상 로그 생성: sagaId={}, currentStep={}",
                sagaContext.getId(), sagaContext.getCurrentStep());

        try {
            // Step별 보상 로그 생성 (현재 단계까지만)
            // stepNumber는 역순 실행을 위해 높은 숫자가 먼저 실행됨

            if (sagaContext.getCurrentStep().ordinal() >= SagaStep.FINALIZE_AUCTION.ordinal()) {
                // Step 3: FINALIZE_AUCTION → 경매 재오픈
                Map<String, Object> payload = new HashMap<>();
                payload.put("auctionId", sagaContext.getAuctionId().toString());
                payload.put("reason", "ORDER_CREATION_FAILED");

                compensationExecutor.createCompensationLog(
                        sagaContext.getId(),
                        "AUCTION_END_SAGA",
                        "AUCTION",
                        sagaContext.getAuctionId(),
                        CompensationType.REOPEN_AUCTION,
                        3,
                        objectMapper.writeValueAsString(payload)
                );
            }

            if (sagaContext.getCurrentStep().ordinal() >= SagaStep.MARK_WINNING_BID.ordinal()) {
                // Step 2: MARK_WINNING_BID → 입찰 상태 복원
                Map<String, Object> payload = new HashMap<>();
                payload.put("bidId", sagaContext.getWinningBidId().toString());
                payload.put("auctionId", sagaContext.getAuctionId().toString());

                compensationExecutor.createCompensationLog(
                        sagaContext.getId(),
                        "AUCTION_END_SAGA",
                        "BID",
                        sagaContext.getWinningBidId(),
                        CompensationType.REVERT_BID_STATUS,
                        2,
                        objectMapper.writeValueAsString(payload)
                );
            }

            if (sagaContext.getOrderId() != null) {
                // Step 1: CREATE_ORDER → 주문 취소
                Map<String, Object> payload = new HashMap<>();
                payload.put("orderId", sagaContext.getOrderId().toString());
                payload.put("auctionId", sagaContext.getAuctionId().toString());
                payload.put("reason", "SAGA_COMPENSATION");

                compensationExecutor.createCompensationLog(
                        sagaContext.getId(),
                        "AUCTION_END_SAGA",
                        "ORDER",
                        sagaContext.getOrderId(),
                        CompensationType.CANCEL_ORDER,
                        1,
                        objectMapper.writeValueAsString(payload)
                );
            }

            // TODO: 추가 보상 액션 등록 - CANCEL_PAYMENT: 결제 취소
            // 결제가 완료된 상태에서 Saga 실패 시 결제 취소 필요
            // paymentId가 있는 경우에만 실행
            // Note: 현재 AuctionEndSaga는 결제 전 단계이므로 일반적으로 필요 없음
            // 하지만 향후 확장을 위해 주석으로 남김
            //
            // if (sagaContext.getPaymentId() != null) {
            //     Map<String, Object> payload = new HashMap<>();
            //     payload.put("paymentId", sagaContext.getPaymentId().toString());
            //     payload.put("orderId", sagaContext.getOrderId().toString());
            //     payload.put("auctionId", sagaContext.getAuctionId().toString());
            //     payload.put("reason", "SAGA_COMPENSATION");
            //
            //     compensationExecutor.createCompensationLog(
            //             sagaContext.getId(),
            //             "AUCTION_END_SAGA",
            //             "PAYMENT",
            //             sagaContext.getPaymentId(),
            //             CompensationType.CANCEL_PAYMENT,
            //             0, // 가장 먼저 실행 (Step 1 전)
            //             objectMapper.writeValueAsString(payload)
            //     );
            // }

            log.info("[AuctionEndSaga] 보상 로그 생성 완료: sagaId={}", sagaContext.getId());

        } catch (Exception e) {
            log.error("[AuctionEndSaga] 보상 로그 생성 실패: sagaId={}, error={}",
                    sagaContext.getId(), e.getMessage(), e);
            throw new RuntimeException("보상 로그 생성 실패", e);
        }
    }

    // ========================================
    // SAGA-005: Resilience4j Fallback Methods
    // ========================================

    /**
     * startAuctionEndSaga Bulkhead Fallback
     * Bulkhead 포화 시 호출 (동시 실행 10개 초과)
     */
    private UUID startAuctionEndSagaFallback(UUID auctionId, Exception e) {
        log.error("[AuctionEndSaga] Bulkhead 포화 - Saga 시작 실패: auctionId={}, error={}",
                auctionId, e.getMessage());
        throw new AuctionDomainException(AuctionErrorCode.SAGA_BULKHEAD_FULL);
    }

    /**
     * executeCreateOrderStep CircuitBreaker Fallback
     * Circuit Breaker OPEN 또는 Order Service 장애 시 호출
     */
    private void executeCreateOrderStepFallback(AuctionEndSagaContext sagaContext, Exception e) {
        log.error("[AuctionEndSaga] Order Service Circuit Breaker 활성화: sagaId={}, error={}",
                sagaContext.getId(), e.getMessage());

        // 예외를 다시 던져서 상위 try-catch에서 보상 트랜잭션을 실행하도록 함
        throw new RuntimeException("Order Service unavailable: " + e.getMessage(), e);
    }
}
