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
 * [Senior's Guide: 역할]
 * 이 클래스는 경매가 종료된 후의 복잡한 후속 처리를 "Saga 패턴"으로 조율하는 지휘자(Orchestrator)입니다.
 * [경매 종료 → 주문 생성 → 낙찰 처리 → 알림 발행] 흐름을 4단계로 나누어 실행하며,
 * 중간에 실패하면 보상 트랜잭션(Compensation)을 역순으로 실행하여 일관성을 복구합니다.
 *
 * [도메인 흐름에서의 위치]
 * 경매 라이프사이클의 [종료 및 낙찰 단계]에 속하며, AuctionScheduler가 경매를 종료한 직후 시작됩니다.
 * - 트리거: AuctionScheduler.endExpiredAuctions() → auction.end() → startAuctionEndSaga()
 * - 목표: 낙찰자에게 주문을 생성하고, 경매를 확정하며, 모든 참여자에게 알립니다
 * - 예외 처리: 주문 생성 실패 시 경매를 재오픈하여 공정성을 보장합니다
 *
 * [왜 Saga 패턴을 사용했을까?]
 *
 * 1. 분산 트랜잭션 문제
 *    - Auction Service와 Order Service는 별도의 마이크로서비스이며, 각자 다른 DB를 사용합니다
 *    - 일반적인 @Transactional로는 두 서비스를 하나의 트랜잭션으로 묶을 수 없습니다
 *    - 예: 경매 종료 후 주문 생성 요청 → Order Service 장애 → 경매는 종료됐지만 주문은 없음 (데이터 불일치!)
 *
 * 2. Saga 패턴의 해결책
 *    - 각 단계를 작은 "로컬 트랜잭션"으로 쪼개고, 상태(SagaContext)를 DB에 저장하여 진행 상황을 추적합니다
 *    - 중간에 실패하면 이미 완료된 단계를 "보상 트랜잭션"으로 되돌립니다 (Compensating Transaction)
 *    - 예: Step 1 성공 → Step 2 실패 → Step 1 보상 실행 → 원래 상태로 복구
 *
 * 3. @CircuitBreaker, @Retry, @Bulkhead (Resilience4j)
 *    - @CircuitBreaker: Order Service가 계속 실패하면 Circuit을 열어서 빠르게 실패하고 보상 시작
 *    - @Retry: 일시적 네트워크 오류는 재시도로 해결 (최대 3회)
 *    - @Bulkhead: 동시 실행 Saga 개수 제한하여 리소스 고갈 방지 (최대 10개)
 *    - @TimeLimiter: Saga 전체가 30초 이상 걸리면 타임아웃 (Zombie Saga 방지)
 *
 * [Saga 단계별 설명]
 *
 * Step 1: CREATE_ORDER (주문 생성 요청)
 *    - Order Service에 Kafka로 CREATE_ORDER_REQUESTED 이벤트 발행
 *    - 보상 로그: REOPEN_AUCTION (주문 생성 실패 시 경매 재오픈)
 *    - 왜 먼저?: 주문이 생성되어야 낙찰자가 결제할 수 있습니다
 *
 * Step 2: MARK_WINNING_BID (낙찰 입찰 표시)
 *    - 최고가 입찰의 상태를 ACTIVE → WON으로 변경
 *    - 보상 로그: REVERT_BID_STATUS (WON → ACTIVE로 복원)
 *    - 왜 필요?: 낙찰자가 누구인지 명확히 하고, 입찰 취소를 방지합니다
 *
 * Step 3: FINALIZE_AUCTION (경매 확정)
 *    - 경매 상태를 ACTIVE → SUCCESS로 변경
 *    - 낙찰자 정보(winnerId, winningBidId, finalPrice) 저장
 *    - 보상 로그: 없음 (이 시점에서 실패하면 재시도만 함)
 *
 * Step 4: PUBLISH_END_EVENT (종료 이벤트 발행)
 *    - AUCTION_ENDED 이벤트를 Kafka로 발행 (낙찰자에게 축하 알림)
 *    - 보상 로그: 없음 (알림은 실패해도 재발행하지 않음)
 *
 * [보상 트랜잭션 (Compensation)]
 *
 * 언제 실행되나요?
 * - Order Service가 ORDER_CREATION_FAILED 이벤트를 보낼 때
 * - AuctionEndSagaMessageHandler.handleOrderCreationFailed() → executeCompensations()
 *
 * 무엇을 복구하나요?
 * 1. REOPEN_AUCTION: 경매를 ACTIVE 상태로 되돌리고 endTime을 +24시간 연장
 * 2. REVERT_BID_STATUS: 낙찰 입찰을 WON → ACTIVE로 복원 (다시 입찰 가능)
 * 3. CANCEL_ORDER: Order Service에 주문 취소 요청 (이미 생성된 경우)
 *
 * 역순 실행 (LIFO):
 * - Step 3 보상 → Step 2 보상 → Step 1 보상 (생성의 역순으로 되돌림)
 *
 * [서비스 재시작 시 복구 (Saga Recovery)]
 *
 * 1. SagaRecoveryService가 @PostConstruct에서 미완료 Saga를 자동 복구합니다
 * 2. PENDING 상태: 처음부터 재실행
 * 3. IN_PROGRESS 상태: 현재 단계(currentStep)부터 재개
 * 4. Zombie Saga (10분 이상 멈춤): 보상 트랜잭션 실행 후 FAILED 상태로 변경
 *
 * [어디서 이 클래스를 사용하나요?]
 * - 트리거: {@link com.bidket.auction.application.auction.scheduler.AuctionScheduler}
 * - 이벤트 수신: {@link AuctionEndSagaMessageHandler} (ORDER_CREATED, ORDER_CREATION_FAILED)
 * - 보상 실행: {@link com.bidket.auction.application.compensation.CompensationExecutor}
 * - 복구: {@link SagaRecoveryService}
 *
 * [신입 개발자 주의사항]
 * - Saga는 "결과적 일관성(Eventual Consistency)"을 제공합니다 (즉시 일관성 아님)
 * - 각 단계는 멱등성(Idempotency)을 보장해야 합니다 (중복 실행되어도 안전해야 함)
 * - SagaContext 상태를 먼저 저장한 후 비즈니스 로직을 실행하세요 (장애 시 복구 가능)
 * - Circuit이 열리면 빠르게 보상으로 진행하므로, 불필요한 재시도를 줄입니다
 * - Saga 실행 중 예외 발생 시 자동으로 보상이 시작됩니다 (catch에서 compensate() 호출)
 *
 * [BACKLOG.md 참조]
 * - SAGA-001 (line 707-762): 경매 종료 Saga 상세 스펙
 * - COMP-001~003: 보상 트랜잭션 구현 스펙
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
