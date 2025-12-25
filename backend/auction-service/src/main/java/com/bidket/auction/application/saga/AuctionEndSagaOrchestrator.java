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

    @Transactional
    public UUID startAuctionEndSaga(UUID auctionId) {
        log.info("[AuctionEndSaga] Saga 시작: auctionId={}", auctionId);

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        Bid winningBid = bidRepository.findHighestBidByAuctionId(auctionId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.NO_BIDS_FOUND));

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

        sagaContext.start();
        sagaContext = sagaRepository.save(sagaContext);

        try {
             
            executeCreateOrderStep(sagaContext);

            log.info("[AuctionEndSaga] Saga 시작 완료: sagaId={}, auctionId={}", sagaContext.getId(), auctionId);

        } catch (Exception e) {
            log.error("[AuctionEndSaga] Saga 실패: sagaId={}, error={}",
                    sagaContext.getId(), e.getMessage(), e);

            sagaContext.fail(e.getMessage());
            sagaRepository.save(sagaContext);

            compensate(sagaContext.getId(), "AuctionEndSagaFailed: " + e.getMessage());

            throw e;
        }

        return sagaContext.getId();
    }

    @Transactional
    @CircuitBreaker(name = "orderService", fallbackMethod = "executeCreateOrderStepFallback")
    @Retry(name = "sagaStep")
    public void executeCreateOrderStep(AuctionEndSagaContext sagaContext) {
        log.info("[AuctionEndSaga] Step 1 시작: CREATE_ORDER, sagaId={}", sagaContext.getId());

        if (sagaContext.getCurrentStep() != SagaStep.CREATE_ORDER) {
            log.warn("[AuctionEndSaga] 잘못된 단계: expected=CREATE_ORDER, actual={}", sagaContext.getCurrentStep());
            return;
        }

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

    @Transactional
    public void executeMarkWinningBidStep(AuctionEndSagaContext sagaContext) {
        log.info("[AuctionEndSaga] Step 2 시작: MARK_WINNING_BID, sagaId={}", sagaContext.getId());

        if (sagaContext.getCurrentStep() != SagaStep.MARK_WINNING_BID) {
            log.warn("[AuctionEndSaga] 잘못된 단계: expected=MARK_WINNING_BID, actual={}", sagaContext.getCurrentStep());
            return;
        }

        Bid winningBid = bidRepository.findById(sagaContext.getWinningBidId())
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.BID_NOT_FOUND));

        winningBid.markAsWon();
        bidRepository.save(winningBid);

        sagaContext.proceedToNextStep();
        sagaRepository.save(sagaContext);

        log.info("[AuctionEndSaga] Step 2 완료: Bid status=WON, bidId={}, sagaId={}",
                winningBid.getId(), sagaContext.getId());

        executeFinalizeAuctionStep(sagaContext);
    }

    @Transactional
    public void executeFinalizeAuctionStep(AuctionEndSagaContext sagaContext) {
        log.info("[AuctionEndSaga] Step 3 시작: FINALIZE_AUCTION, sagaId={}", sagaContext.getId());

        if (sagaContext.getCurrentStep() != SagaStep.FINALIZE_AUCTION) {
            log.warn("[AuctionEndSaga] 잘못된 단계: expected=FINALIZE_AUCTION, actual={}", sagaContext.getCurrentStep());
            return;
        }

        Auction auction = auctionRepository.findById(sagaContext.getAuctionId())
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND));

        auction.end(true);
        auction.setWinner(
                sagaContext.getWinnerId(),
                sagaContext.getWinningBidId(),
                sagaContext.getFinalPrice()
        );
        auctionRepository.save(auction);

        sagaContext.proceedToNextStep();
        sagaRepository.save(sagaContext);

        log.info("[AuctionEndSaga] Step 3 완료: Auction status=SUCCESS, auctionId={}, sagaId={}",
                auction.getId(), sagaContext.getId());

        executePublishEndEventStep(sagaContext);
    }

    @Transactional
    public void executePublishEndEventStep(AuctionEndSagaContext sagaContext) {
        log.info("[AuctionEndSaga] Step 4 시작: PUBLISH_END_EVENT, sagaId={}", sagaContext.getId());

        if (sagaContext.getCurrentStep() != SagaStep.PUBLISH_END_EVENT) {
            log.warn("[AuctionEndSaga] 잘못된 단계: expected=PUBLISH_END_EVENT, actual={}", sagaContext.getCurrentStep());
            return;
        }

        LocalDateTime closedAt = LocalDateTime.now();

        notificationEventProducer.publishWinnerNotification(
                sagaContext.getWinnerId(),
                sagaContext.getAuctionId(),
                sagaContext.getFinalPrice(),
                closedAt,
                sagaContext.getCorrelationId()
        );

        log.info("[AuctionEndSaga] 낙찰자 알림 발행 완료: winnerId={}, auctionId={}",
                sagaContext.getWinnerId(), sagaContext.getAuctionId());

        List<Bid> allBids = bidRepository.findByAuctionId(sagaContext.getAuctionId());
        int loserCount = 0;

        for (Bid bid : allBids) {
             
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

        sagaContext.complete();
        sagaRepository.save(sagaContext);

        log.info("[AuctionEndSaga] Saga 완료: sagaId={}, auctionId={}, status=COMPLETED, totalNotifications={}",
                sagaContext.getId(), sagaContext.getAuctionId(), loserCount + 1);
    }

    @Transactional
    @Bulkhead(name = "sagaCompensation")
    public void compensate(UUID sagaId, String failureReason) {
        log.warn("[AuctionEndSaga] 보상 트랜잭션 시작: sagaId={}, reason={}", sagaId, failureReason);

        AuctionEndSagaContext sagaContext = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new AuctionDomainException(AuctionErrorCode.SAGA_NOT_FOUND));

        sagaContext.startCompensation(failureReason);
        sagaRepository.save(sagaContext);

        try {
             
            createCompensationLogs(sagaContext);

            compensationExecutor.executeCompensations(sagaId, failureReason);

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

    private void createCompensationLogs(AuctionEndSagaContext sagaContext) {
        log.info("[AuctionEndSaga] 보상 로그 생성: sagaId={}, currentStep={}",
                sagaContext.getId(), sagaContext.getCurrentStep());

        try {
             
             

            if (sagaContext.getCurrentStep().ordinal() >= SagaStep.FINALIZE_AUCTION.ordinal()) {
                 
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

            log.info("[AuctionEndSaga] 보상 로그 생성 완료: sagaId={}", sagaContext.getId());

        } catch (Exception e) {
            log.error("[AuctionEndSaga] 보상 로그 생성 실패: sagaId={}, error={}",
                    sagaContext.getId(), e.getMessage(), e);
            throw new RuntimeException("보상 로그 생성 실패", e);
        }
    }


    private void executeCreateOrderStepFallback(AuctionEndSagaContext sagaContext, Exception e) {
        log.error("[AuctionEndSaga] Order Service Circuit Breaker 활성화: sagaId={}, error={}",
                sagaContext.getId(), e.getMessage());

        throw new RuntimeException("Order Service unavailable: " + e.getMessage(), e);
    }
}
