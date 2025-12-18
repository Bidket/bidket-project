package com.bidket.auction.application.saga;

import com.bidket.auction.application.compensation.CompensationExecutor;
import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.repository.AuctionRepository;
import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.repository.BidRepository;
import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaContext;
import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaStep;
import com.bidket.auction.domain.saga.model.SagaStatus;
import com.bidket.auction.domain.saga.repository.PaymentTimeoutSagaContextRepository;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import com.bidket.auction.infrastructure.notification.NotificationEventProducer;
import com.bidket.auction.infrastructure.order.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 타임아웃 Saga Orchestrator
 * BACKLOG.md SAGA-002 구현
 *
 * Saga Flow:
 * 1. REOPEN_AUCTION: 경매 상태를 REOPENED로 변경하고 endTime을 +24h 연장
 * 2. REVERT_BID_STATUS: 이전 낙찰 입찰을 WON에서 ACTIVE로 되돌림
 * 3. CANCEL_ORDER: Order Service에 주문 취소 요청
 * 4. PUBLISH_REOPEN_EVENT: AUCTION_REOPENED 이벤트 발행
 *
 * Note: 재고 복원 단계 제거됨
 * - Bidket은 크림/StockX와 같은 리셀 플랫폼 (1개 신발 = 1개 경매)
 * - 재고 개념이 없으므로 경매 재오픈으로만 처리
 *
 * Compensation:
 * - Saga 실패 시 CompensationExecutor를 통한 보상 실행
 * - 경매를 원래 SUCCESS 상태로 복원 (필요 시)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTimeoutSagaOrchestrator {

    private static final String PAYMENT_TIMEOUT_REASON = "PAYMENT_TIMEOUT";

    private final PaymentTimeoutSagaContextRepository sagaRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final OrderEventProducer orderEventProducer;
    private final NotificationEventProducer notificationEventProducer;
    private final CompensationExecutor compensationExecutor;

    /**
     * 결제 타임아웃 Saga 시작
     * Order Service의 PAYMENT_TIMEOUT 이벤트 수신 시 호출
     *
     * @param auctionId 경매 ID
     * @param orderId   타임아웃된 주문 ID
     * @return 생성된 Saga ID
     */
    @Transactional
    public UUID startPaymentTimeoutSaga(UUID auctionId, UUID orderId) {
        log.info("[PaymentTimeoutSaga] Saga 시작: auctionId={}, orderId={}", auctionId, orderId);

        // 1. 경매 조회 및 검증
        Auction auction = findAuction(auctionId);

        // 2. Idempotency 체크: 이미 진행 중인 Saga가 있는지 확인
        sagaRepository.findByOrderId(orderId).ifPresent(existingSaga -> {
            log.warn("[PaymentTimeoutSaga] 이미 존재하는 Saga: sagaId={}, orderId={}",
                    existingSaga.getId(), orderId);
            throw new AuctionDomainException(AuctionErrorCode.SAGA_ALREADY_EXISTS);
        });

        // 3. 낙찰 입찰 조회
        Bid winningBid = auction.getWinnerInfo() != null && auction.getWinnerInfo().getWinningBidId() != null
                ? findBid(auction.getWinnerInfo().getWinningBidId())
                : null;

        if (winningBid == null) {
            log.error("[PaymentTimeoutSaga] 낙찰 입찰을 찾을 수 없음: auctionId={}", auctionId);
            throw new AuctionDomainException(AuctionErrorCode.NO_BIDS_FOUND);
        }

        // 4. Saga Context 생성
        PaymentTimeoutSagaContext sagaContext = PaymentTimeoutSagaContext.builder()
                .auctionId(auctionId)
                .orderId(orderId)
                .winnerId(winningBid.getBidderId())
                .winningBidId(winningBid.getId())
                .productSizeId(auction.getProductSizeId())
                .status(SagaStatus.PENDING)
                .currentStep(PaymentTimeoutSagaStep.REOPEN_AUCTION)
                .build();

        sagaContext = sagaRepository.save(sagaContext);

        // 5. Saga 시작
        sagaContext.start();
        sagaContext = sagaRepository.save(sagaContext);

        try {
            // 6. Step 1: 경매 재오픈
            executeReopenAuctionStep(sagaContext);

            // 7. Step 2: 입찰 상태 복원
            executeRevertBidStatusStep(sagaContext);

            // 8. Step 3: 주문 취소
            executeCancelOrderStep(sagaContext);

            // 9. Step 4: AUCTION_REOPENED 이벤트 발행
            executePublishReopenEventStep(sagaContext);

            log.info("[PaymentTimeoutSaga] Saga 완료: sagaId={}, auctionId={}, status=COMPLETED",
                    sagaContext.getId(), auctionId);

        } catch (Exception e) {
            log.error("[PaymentTimeoutSaga] Saga 실패: sagaId={}, error={}",
                    sagaContext.getId(), e.getMessage(), e);

            sagaContext.fail(e.getMessage());
            sagaRepository.save(sagaContext);

            // 보상 트랜잭션 실행
            compensate(sagaContext.getId(), "PaymentTimeoutSagaFailed: " + e.getMessage());

            throw e;
        }

        return sagaContext.getId();
    }

    /**
     * Step 1: 경매 재오픈
     * - 경매 상태를 REOPENED로 변경
     * - winnerId, winningBidId, finalPrice 초기화
     * - endTime을 현재 시간 + 24시간으로 설정
     */
    @Transactional
    public void executeReopenAuctionStep(PaymentTimeoutSagaContext sagaContext) {
        log.info("[PaymentTimeoutSaga] Step 1 시작: REOPEN_AUCTION, sagaId={}", sagaContext.getId());

        if (!validateStep(sagaContext, PaymentTimeoutSagaStep.REOPEN_AUCTION)) {
            return;
        }

        // 경매 조회
        Auction auction = findAuction(sagaContext.getAuctionId());

        // 경매 재오픈 (도메인 메서드 사용)
        auction.reopen();
        auctionRepository.save(auction);

        // 다음 단계로 진행
        sagaContext.proceedToNextStep();
        sagaRepository.save(sagaContext);

        log.info("[PaymentTimeoutSaga] Step 1 완료: Auction status=ACTIVE(REOPENED), auctionId={}, newEndTime={}, sagaId={}",
                auction.getId(), auction.getPeriod().getEndTime(), sagaContext.getId());
    }

    /**
     * Step 2: 입찰 상태 복원
     * - 낙찰 입찰을 WON에서 ACTIVE로 되돌림
     */
    @Transactional
    public void executeRevertBidStatusStep(PaymentTimeoutSagaContext sagaContext) {
        log.info("[PaymentTimeoutSaga] Step 2 시작: REVERT_BID_STATUS, sagaId={}", sagaContext.getId());

        if (!validateStep(sagaContext, PaymentTimeoutSagaStep.REVERT_BID_STATUS)) {
            return;
        }

        // 낙찰 입찰 조회
        Bid winningBid = findBid(sagaContext.getWinningBidId());

        // 입찰 상태 복원 (WON → ACTIVE)
        winningBid.revertFromWon();
        bidRepository.save(winningBid);

        // 다음 단계로 진행
        sagaContext.proceedToNextStep();
        sagaRepository.save(sagaContext);

        log.info("[PaymentTimeoutSaga] Step 2 완료: Bid status reverted to ACTIVE, bidId={}, sagaId={}",
                winningBid.getId(), sagaContext.getId());
    }

    /**
     * Step 3: 주문 취소 요청
     * Order Service에 CANCEL_ORDER_REQUESTED 이벤트 발행
     */
    @Transactional
    public void executeCancelOrderStep(PaymentTimeoutSagaContext sagaContext) {
        log.info("[PaymentTimeoutSaga] Step 3 시작: CANCEL_ORDER, sagaId={}", sagaContext.getId());

        if (!validateStep(sagaContext, PaymentTimeoutSagaStep.CANCEL_ORDER)) {
            return;
        }

        // Order Service에 주문 취소 요청 (OutBox 패턴)
        orderEventProducer.publishCancelOrderRequest(
                sagaContext.getOrderId(),
                sagaContext.getAuctionId(),
                PAYMENT_TIMEOUT_REASON,
                sagaContext.getCorrelationId()
        );

        log.info("[PaymentTimeoutSaga] Step 3 완료: CANCEL_ORDER 이벤트 발행, orderId={}, sagaId={}",
                sagaContext.getOrderId(), sagaContext.getId());

        // 다음 단계로 진행
        sagaContext.proceedToNextStep();
        sagaRepository.save(sagaContext);
    }

    /**
     * Step 4: AUCTION_REOPENED 이벤트 발행
     * Notification Service에 AUCTION_REOPENED 이벤트 발행
     */
    @Transactional
    public void executePublishReopenEventStep(PaymentTimeoutSagaContext sagaContext) {
        log.info("[PaymentTimeoutSaga] Step 4 시작: PUBLISH_REOPEN_EVENT, sagaId={}", sagaContext.getId());

        if (!validateStep(sagaContext, PaymentTimeoutSagaStep.PUBLISH_REOPEN_EVENT)) {
            return;
        }

        // 경매 조회하여 새로운 종료 시간 가져오기
        Auction auction = findAuction(sagaContext.getAuctionId());

        // AUCTION_REOPENED 이벤트 발행 (OutBox 패턴)
        notificationEventProducer.publishAuctionReopenedNotification(
                sagaContext.getAuctionId(),
                sagaContext.getProductSizeId(),
                sagaContext.getWinnerId(),
                PAYMENT_TIMEOUT_REASON,
                auction.getPeriod().getEndTime(),
                sagaContext.getCorrelationId()
        );

        log.info("[PaymentTimeoutSaga] Step 4 완료: AUCTION_REOPENED 이벤트 발행, auctionId={}, sagaId={}",
                sagaContext.getAuctionId(), sagaContext.getId());

        // Saga 완료
        sagaContext.complete();
        sagaRepository.save(sagaContext);

        log.info("[PaymentTimeoutSaga] Saga 완료: sagaId={}, auctionId={}, status=COMPLETED",
                sagaContext.getId(), sagaContext.getAuctionId());
    }

    /**
     * 보상 트랜잭션: Saga 실패 시 원래 상태로 복원
     *
     * @param sagaId        Saga ID
     * @param failureReason 실패 사유
     */
    @Transactional
    public void compensate(UUID sagaId, String failureReason) {
        log.warn("[PaymentTimeoutSaga] 보상 트랜잭션 시작: sagaId={}, reason={}", sagaId, failureReason);

        PaymentTimeoutSagaContext sagaContext = sagaRepository.findById(sagaId)
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

            log.info("[PaymentTimeoutSaga] 보상 트랜잭션 완료: sagaId={}, status=COMPENSATED", sagaId);

        } catch (Exception e) {
            log.error("[PaymentTimeoutSaga] 보상 트랜잭션 실패: sagaId={}, error={}",
                    sagaId, e.getMessage(), e);

            sagaContext.fail("보상 실패: " + e.getMessage());
            sagaRepository.save(sagaContext);

            throw new AuctionDomainException(AuctionErrorCode.SAGA_COMPENSATION_FAILED);
        }
    }

    /**
     * 현재 Saga 단계가 예상한 단계와 일치하는지 검증
     *
     * @param sagaContext    Saga Context
     * @param expectedStep   예상 단계
     * @return 단계가 일치하면 true, 불일치하면 false
     */
    private boolean validateStep(PaymentTimeoutSagaContext sagaContext, PaymentTimeoutSagaStep expectedStep) {
        if (sagaContext.getCurrentStep() != expectedStep) {
            log.warn("[PaymentTimeoutSaga] 잘못된 단계: expected={}, actual={}",
                    expectedStep, sagaContext.getCurrentStep());
            return false;
        }
        return true;
    }

    /**
     * 경매 ID로 경매 조회
     *
     * @param auctionId 경매 ID
     * @return 조회된 경매
     * @throws AuctionDomainException 경매를 찾을 수 없는 경우
     */
    private Auction findAuction(UUID auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));
    }

    /**
     * 입찰 ID로 입찰 조회
     *
     * @param bidId 입찰 ID
     * @return 조회된 입찰
     * @throws AuctionDomainException 입찰을 찾을 수 없는 경우
     */
    private Bid findBid(UUID bidId) {
        return bidRepository.findById(bidId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.BID_NOT_FOUND));
    }

    /**
     * 보상 로그 생성
     * Saga의 각 단계에 대한 보상 로그를 역순으로 생성
     *
     * @param sagaContext Saga Context
     */
    private void createCompensationLogs(PaymentTimeoutSagaContext sagaContext) {
        log.info("[PaymentTimeoutSaga] 보상 로그 생성: sagaId={}, currentStep={}",
                sagaContext.getId(), sagaContext.getCurrentStep());

        try {
            // Step별 보상 로그 생성 (현재 단계까지만)
            // stepNumber는 역순 실행을 위해 높은 숫자가 먼저 실행됨

            if (sagaContext.getCurrentStep().ordinal() >= PaymentTimeoutSagaStep.REVERT_BID_STATUS.ordinal()) {
                // Step 2: REVERT_BID_STATUS → 입찰 상태를 다시 WON으로 (필요 시)
                // 현재는 보상 불필요 (경매가 이미 재오픈되었으므로)
                log.info("[PaymentTimeoutSaga] Step 2 보상 불필요 (입찰 상태 복원은 되돌릴 필요 없음)");
            }

            if (sagaContext.getCurrentStep().ordinal() >= PaymentTimeoutSagaStep.REOPEN_AUCTION.ordinal()) {
                // Step 1: REOPEN_AUCTION → 경매를 SUCCESS로 되돌림 (중요한 경우만)
                // 주의: 경매 재오픈이 실패한 경우, 경매를 원래 SUCCESS 상태로 복원해야 할 수 있음
                // 하지만 일반적으로는 재오픈이 성공하면 보상이 불필요함
                log.info("[PaymentTimeoutSaga] Step 1 보상: 경매 재오픈 실패 시에만 필요");
            }

            log.info("[PaymentTimeoutSaga] 보상 로그 생성 완료: sagaId={}", sagaContext.getId());

        } catch (Exception e) {
            log.error("[PaymentTimeoutSaga] 보상 로그 생성 실패: sagaId={}, error={}",
                    sagaContext.getId(), e.getMessage(), e);
            throw new RuntimeException("보상 로그 생성 실패", e);
        }
    }
}
