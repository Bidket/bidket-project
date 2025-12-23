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

    @Transactional
    public UUID startPaymentTimeoutSaga(UUID auctionId, UUID orderId) {
        log.info("[PaymentTimeoutSaga] Saga 시작: auctionId={}, orderId={}", auctionId, orderId);

        Auction auction = findAuction(auctionId);

        sagaRepository.findByOrderId(orderId).ifPresent(existingSaga -> {
            log.warn("[PaymentTimeoutSaga] 이미 존재하는 Saga: sagaId={}, orderId={}",
                    existingSaga.getId(), orderId);
            throw new AuctionDomainException(AuctionErrorCode.SAGA_ALREADY_EXISTS);
        });

        Bid winningBid = auction.getWinnerInfo() != null && auction.getWinnerInfo().getWinningBidId() != null
                ? findBid(auction.getWinnerInfo().getWinningBidId())
                : null;

        if (winningBid == null) {
            log.error("[PaymentTimeoutSaga] 낙찰 입찰을 찾을 수 없음: auctionId={}", auctionId);
            throw new AuctionDomainException(AuctionErrorCode.NO_BIDS_FOUND);
        }

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

        sagaContext.start();
        sagaContext = sagaRepository.save(sagaContext);

        try {
             
            executeReopenAuctionStep(sagaContext);

            executeRevertBidStatusStep(sagaContext);

            executeCancelOrderStep(sagaContext);

            executePublishReopenEventStep(sagaContext);

            log.info("[PaymentTimeoutSaga] Saga 완료: sagaId={}, auctionId={}, status=COMPLETED",
                    sagaContext.getId(), auctionId);

        } catch (Exception e) {
            log.error("[PaymentTimeoutSaga] Saga 실패: sagaId={}, error={}",
                    sagaContext.getId(), e.getMessage(), e);

            sagaContext.fail(e.getMessage());
            sagaRepository.save(sagaContext);

            compensate(sagaContext.getId(), "PaymentTimeoutSagaFailed: " + e.getMessage());

            throw e;
        }

        return sagaContext.getId();
    }

    @Transactional
    public void executeReopenAuctionStep(PaymentTimeoutSagaContext sagaContext) {
        log.info("[PaymentTimeoutSaga] Step 1 시작: REOPEN_AUCTION, sagaId={}", sagaContext.getId());

        if (!validateStep(sagaContext, PaymentTimeoutSagaStep.REOPEN_AUCTION)) {
            return;
        }

        Auction auction = findAuction(sagaContext.getAuctionId());

        auction.reopen();
        auctionRepository.save(auction);

        sagaContext.proceedToNextStep();
        sagaRepository.save(sagaContext);

        log.info("[PaymentTimeoutSaga] Step 1 완료: Auction status=ACTIVE(REOPENED), auctionId={}, newEndTime={}, sagaId={}",
                auction.getId(), auction.getPeriod().getEndTime(), sagaContext.getId());
    }

    @Transactional
    public void executeRevertBidStatusStep(PaymentTimeoutSagaContext sagaContext) {
        log.info("[PaymentTimeoutSaga] Step 2 시작: REVERT_BID_STATUS, sagaId={}", sagaContext.getId());

        if (!validateStep(sagaContext, PaymentTimeoutSagaStep.REVERT_BID_STATUS)) {
            return;
        }

        Bid winningBid = findBid(sagaContext.getWinningBidId());

        winningBid.revertFromWon();
        bidRepository.save(winningBid);

        sagaContext.proceedToNextStep();
        sagaRepository.save(sagaContext);

        log.info("[PaymentTimeoutSaga] Step 2 완료: Bid status reverted to ACTIVE, bidId={}, sagaId={}",
                winningBid.getId(), sagaContext.getId());
    }

    @Transactional
    public void executeCancelOrderStep(PaymentTimeoutSagaContext sagaContext) {
        log.info("[PaymentTimeoutSaga] Step 3 시작: CANCEL_ORDER, sagaId={}", sagaContext.getId());

        if (!validateStep(sagaContext, PaymentTimeoutSagaStep.CANCEL_ORDER)) {
            return;
        }

        orderEventProducer.publishCancelOrderRequest(
                sagaContext.getOrderId(),
                sagaContext.getAuctionId(),
                sagaContext.getWinnerId(),
                PAYMENT_TIMEOUT_REASON,
                sagaContext.getCorrelationId()
        );

        log.info("[PaymentTimeoutSaga] Step 3 완료: CANCEL_ORDER 이벤트 발행, orderId={}, sagaId={}",
                sagaContext.getOrderId(), sagaContext.getId());

        sagaContext.proceedToNextStep();
        sagaRepository.save(sagaContext);
    }

    @Transactional
    public void executePublishReopenEventStep(PaymentTimeoutSagaContext sagaContext) {
        log.info("[PaymentTimeoutSaga] Step 4 시작: PUBLISH_REOPEN_EVENT, sagaId={}", sagaContext.getId());

        if (!validateStep(sagaContext, PaymentTimeoutSagaStep.PUBLISH_REOPEN_EVENT)) {
            return;
        }

        Auction auction = findAuction(sagaContext.getAuctionId());

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

        sagaContext.complete();
        sagaRepository.save(sagaContext);

        log.info("[PaymentTimeoutSaga] Saga 완료: sagaId={}, auctionId={}, status=COMPLETED",
                sagaContext.getId(), sagaContext.getAuctionId());
    }

    @Transactional
    public void compensate(UUID sagaId, String failureReason) {
        log.warn("[PaymentTimeoutSaga] 보상 트랜잭션 시작: sagaId={}, reason={}", sagaId, failureReason);

        PaymentTimeoutSagaContext sagaContext = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.SAGA_NOT_FOUND));

        sagaContext.startCompensation(failureReason);
        sagaRepository.save(sagaContext);

        try {
             
            createCompensationLogs(sagaContext);

            compensationExecutor.executeCompensations(sagaId, failureReason);

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

    private boolean validateStep(PaymentTimeoutSagaContext sagaContext, PaymentTimeoutSagaStep expectedStep) {
        if (sagaContext.getCurrentStep() != expectedStep) {
            log.warn("[PaymentTimeoutSaga] 잘못된 단계: expected={}, actual={}",
                    expectedStep, sagaContext.getCurrentStep());
            return false;
        }
        return true;
    }

    private Auction findAuction(UUID auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));
    }

    private Bid findBid(UUID bidId) {
        return bidRepository.findById(bidId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.BID_NOT_FOUND));
    }

    private void createCompensationLogs(PaymentTimeoutSagaContext sagaContext) {
        log.info("[PaymentTimeoutSaga] 보상 로그 생성: sagaId={}, currentStep={}",
                sagaContext.getId(), sagaContext.getCurrentStep());

        try {
             
             

            if (sagaContext.getCurrentStep().ordinal() >= PaymentTimeoutSagaStep.REVERT_BID_STATUS.ordinal()) {
                 
                 
                log.info("[PaymentTimeoutSaga] Step 2 보상 불필요 (입찰 상태 복원은 되돌릴 필요 없음)");
            }

            if (sagaContext.getCurrentStep().ordinal() >= PaymentTimeoutSagaStep.REOPEN_AUCTION.ordinal()) {
                 
                 
                 
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
