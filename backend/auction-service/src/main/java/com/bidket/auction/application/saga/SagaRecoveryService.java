package com.bidket.auction.application.saga;

import com.bidket.auction.application.compensation.CompensationExecutor;
import com.bidket.auction.application.outbox.publisher.OutboxEventPublisher;
import com.bidket.auction.domain.outbox.model.AuctionOutbox;
import com.bidket.auction.domain.outbox.repository.OutboxRepository;
import com.bidket.auction.domain.saga.model.AuctionEndSagaContext;
import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;
import com.bidket.auction.domain.saga.repository.AuctionEndSagaContextRepository;
import com.bidket.auction.domain.saga.repository.PaymentTimeoutSagaContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaRecoveryService {

    private final AuctionEndSagaContextRepository auctionEndSagaRepository;
    private final PaymentTimeoutSagaContextRepository paymentTimeoutSagaRepository;
    private final AuctionEndSagaOrchestrator auctionEndOrchestrator;
    private final PaymentTimeoutSagaOrchestrator paymentTimeoutOrchestrator;
    private final OutboxRepository outboxRepository;
    private final OutboxEventPublisher outboxPublisher;
    private final CompensationExecutor compensationExecutor;
    private final Clock clock;

    @Value("${bidket.saga.recovery.enabled:true}")
    private boolean recoveryEnabled;

    @Value("${bidket.saga.recovery.zombie-timeout-minutes:10}")
    private int zombieTimeoutMinutes;

    @Value("${bidket.saga.recovery.outbox-max-retries:3}")
    private int outboxMaxRetries;

    @Value("${bidket.saga.recovery.outbox-batch-size:100}")
    private int outboxBatchSize;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        if (!recoveryEnabled) {
            log.info("[SagaRecovery] 복구 기능이 비활성화되어 있습니다 (recovery.enabled=false)");
            return;
        }

        log.info("=== [SagaRecovery] 복구 프로세스 시작 ===");
        log.info("[SagaRecovery] 설정: zombieTimeout={}분, outboxMaxRetries={}, batchSize={}",
                zombieTimeoutMinutes, outboxMaxRetries, outboxBatchSize);

        try {
             
            recoverAuctionEndSagas();

            recoverPaymentTimeoutSagas();

            recoverOutboxEvents();

            log.info("=== [SagaRecovery] 복구 프로세스 완료 ===");

        } catch (Exception e) {
            log.error("[SagaRecovery] 복구 프로세스 중 오류 발생", e);
             
        }
    }

    @Transactional
    public void recoverAuctionEndSagas() {
        log.info("[SagaRecovery] AuctionEndSaga 복구 시작");

        List<AuctionEndSagaContext> pendingSagas = auctionEndSagaRepository.findByStatus(SagaStatus.PENDING);
        List<AuctionEndSagaContext> inProgressSagas = auctionEndSagaRepository.findByStatus(SagaStatus.IN_PROGRESS);

        int totalCount = pendingSagas.size() + inProgressSagas.size();
        log.info("[SagaRecovery] AuctionEndSaga 대상: PENDING={}, IN_PROGRESS={}, total={}",
                pendingSagas.size(), inProgressSagas.size(), totalCount);

        if (totalCount == 0) {
            log.info("[SagaRecovery] 복구할 AuctionEndSaga가 없습니다");
            return;
        }

        int recoveredCount = 0;
        int zombieCount = 0;
        int failedCount = 0;

        for (AuctionEndSagaContext saga : pendingSagas) {
            try {
                if (isZombie(saga)) {
                    handleZombieSaga(saga);
                    zombieCount++;
                } else {
                    resumeAuctionEndSaga(saga);
                    recoveredCount++;
                }
            } catch (Exception e) {
                failedCount++;
                log.error("[SagaRecovery] AuctionEndSaga 복구 실패: sagaId={}, error={}",
                        saga.getId(), e.getMessage(), e);
            }
        }

        for (AuctionEndSagaContext saga : inProgressSagas) {
            try {
                if (isZombie(saga)) {
                    handleZombieSaga(saga);
                    zombieCount++;
                } else {
                    resumeAuctionEndSaga(saga);
                    recoveredCount++;
                }
            } catch (Exception e) {
                failedCount++;
                log.error("[SagaRecovery] AuctionEndSaga 복구 실패: sagaId={}, error={}",
                        saga.getId(), e.getMessage(), e);
            }
        }

        log.info("[SagaRecovery] AuctionEndSaga 복구 완료: recovered={}, zombie={}, failed={}",
                recoveredCount, zombieCount, failedCount);
    }

    @Transactional
    public void recoverPaymentTimeoutSagas() {
        log.info("[SagaRecovery] PaymentTimeoutSaga 복구 시작");

        List<PaymentTimeoutSagaContext> pendingSagas = paymentTimeoutSagaRepository.findByStatus(SagaStatus.PENDING);
        List<PaymentTimeoutSagaContext> inProgressSagas = paymentTimeoutSagaRepository.findByStatus(SagaStatus.IN_PROGRESS);

        int totalCount = pendingSagas.size() + inProgressSagas.size();
        log.info("[SagaRecovery] PaymentTimeoutSaga 대상: PENDING={}, IN_PROGRESS={}, total={}",
                pendingSagas.size(), inProgressSagas.size(), totalCount);

        if (totalCount == 0) {
            log.info("[SagaRecovery] 복구할 PaymentTimeoutSaga가 없습니다");
            return;
        }

        int recoveredCount = 0;
        int zombieCount = 0;
        int failedCount = 0;

        for (PaymentTimeoutSagaContext saga : pendingSagas) {
            try {
                if (isPaymentTimeoutZombie(saga)) {
                    handlePaymentTimeoutZombieSaga(saga);
                    zombieCount++;
                } else {
                    resumePaymentTimeoutSaga(saga);
                    recoveredCount++;
                }
            } catch (Exception e) {
                failedCount++;
                log.error("[SagaRecovery] PaymentTimeoutSaga 복구 실패: sagaId={}, error={}",
                        saga.getId(), e.getMessage(), e);
            }
        }

        for (PaymentTimeoutSagaContext saga : inProgressSagas) {
            try {
                if (isPaymentTimeoutZombie(saga)) {
                    handlePaymentTimeoutZombieSaga(saga);
                    zombieCount++;
                } else {
                    resumePaymentTimeoutSaga(saga);
                    recoveredCount++;
                }
            } catch (Exception e) {
                failedCount++;
                log.error("[SagaRecovery] PaymentTimeoutSaga 복구 실패: sagaId={}, error={}",
                        saga.getId(), e.getMessage(), e);
            }
        }

        log.info("[SagaRecovery] PaymentTimeoutSaga 복구 완료: recovered={}, zombie={}, failed={}",
                recoveredCount, zombieCount, failedCount);
    }

    @Transactional
    public void recoverOutboxEvents() {
        log.info("[SagaRecovery] OutBox 이벤트 복구 시작");

        List<AuctionOutbox> readyToPublish = outboxRepository.findReadyToPublish(outboxBatchSize);

        log.info("[SagaRecovery] OutBox 복구 대상: count={}", readyToPublish.size());

        if (readyToPublish.isEmpty()) {
            log.info("[SagaRecovery] 복구할 OutBox 이벤트가 없습니다");
            return;
        }

        int publishedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        LocalDateTime now = LocalDateTime.now(clock);

        for (AuctionOutbox outbox : readyToPublish) {
            try {
                 
                if (outbox.getRetryCount() >= outboxMaxRetries) {
                    log.warn("[SagaRecovery] 최대 재시도 초과: outboxId={}, retryCount={}, maxRetries={}",
                            outbox.getId(), outbox.getRetryCount(), outboxMaxRetries);
                    skippedCount++;
                    continue;
                }

                if (!outbox.canPublish(outboxMaxRetries, now, clock)) {
                    log.debug("[SagaRecovery] 발행 불가 상태: outboxId={}, status={}, retryCount={}",
                            outbox.getId(), outbox.getStatus(), outbox.getRetryCount());
                    skippedCount++;
                    continue;
                }

                log.info("[SagaRecovery] OutBox 재발행: outboxId={}, eventType={}, retryCount={}",
                        outbox.getId(), outbox.getEventType(), outbox.getRetryCount());

                outboxPublisher.publish(outbox);
                publishedCount++;

            } catch (Exception e) {
                failedCount++;
                log.error("[SagaRecovery] OutBox 재발행 실패: outboxId={}, error={}",
                        outbox.getId(), e.getMessage(), e);
            }
        }

        log.info("[SagaRecovery] OutBox 복구 완료: published={}, skipped={}, failed={}",
                publishedCount, skippedCount, failedCount);
    }

    private void resumeAuctionEndSaga(AuctionEndSagaContext saga) {
        log.info("[SagaRecovery] AuctionEndSaga 재개: sagaId={}, status={}, currentStep={}",
                saga.getId(), saga.getStatus(), saga.getCurrentStep());

        try {
             
            if (saga.getStatus() == SagaStatus.PENDING) {
                saga.start();
                auctionEndSagaRepository.save(saga);
            }

            switch (saga.getCurrentStep()) {
                case CREATE_ORDER:
                    auctionEndOrchestrator.executeCreateOrderStep(saga);
                    break;
                case MARK_WINNING_BID:
                    auctionEndOrchestrator.executeMarkWinningBidStep(saga);
                    break;
                case FINALIZE_AUCTION:
                    auctionEndOrchestrator.executeFinalizeAuctionStep(saga);
                    break;
                case PUBLISH_END_EVENT:
                    auctionEndOrchestrator.executePublishEndEventStep(saga);
                    break;
                default:
                    log.warn("[SagaRecovery] 알 수 없는 단계: sagaId={}, step={}",
                            saga.getId(), saga.getCurrentStep());
            }

            log.info("[SagaRecovery] AuctionEndSaga 재개 성공: sagaId={}", saga.getId());

        } catch (Exception e) {
            log.error("[SagaRecovery] AuctionEndSaga 재개 실패: sagaId={}, error={}",
                    saga.getId(), e.getMessage(), e);

            saga.fail("복구 실패: " + e.getMessage());
            auctionEndSagaRepository.save(saga);

            try {
                auctionEndOrchestrator.compensate(saga.getId(), "RecoveryFailed: " + e.getMessage());
            } catch (Exception compensationError) {
                log.error("[SagaRecovery] 보상 실패: sagaId={}, error={}",
                        saga.getId(), compensationError.getMessage(), compensationError);
            }
        }
    }

    private boolean isZombie(AuctionEndSagaContext saga) {
        LocalDateTime updatedAt = saga.getUpdatedAt();
        if (updatedAt == null) {
            updatedAt = saga.getCreatedAt();
        }

        if (updatedAt == null) {
             
            return false;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Duration duration = Duration.between(updatedAt, now);
        long minutesElapsed = duration.toMinutes();

        boolean isZombie = minutesElapsed > zombieTimeoutMinutes;

        if (isZombie) {
            log.warn("[SagaRecovery] Zombie Saga 감지: sagaId={}, status={}, updatedAt={}, minutesElapsed={}",
                    saga.getId(), saga.getStatus(), updatedAt, minutesElapsed);
        }

        return isZombie;
    }

    private void handleZombieSaga(AuctionEndSagaContext saga) {
        log.warn("[SagaRecovery] Zombie Saga 처리 시작: sagaId={}, status={}, currentStep={}",
                saga.getId(), saga.getStatus(), saga.getCurrentStep());

        try {
             
            saga.fail("Zombie 트랜잭션 감지: " + zombieTimeoutMinutes + "분 이상 진행 없음");
            auctionEndSagaRepository.save(saga);

            compensationExecutor.executeCompensations(saga.getId(), "ZombieTransactionDetected");

            log.warn("[SagaRecovery] Zombie Saga 처리 완료: sagaId={}, status=FAILED",
                    saga.getId());

        } catch (Exception e) {
            log.error("[SagaRecovery] Zombie Saga 처리 실패: sagaId={}, error={}",
                    saga.getId(), e.getMessage(), e);
        }
    }

    private void resumePaymentTimeoutSaga(PaymentTimeoutSagaContext saga) {
        log.info("[SagaRecovery] PaymentTimeoutSaga 재개: sagaId={}, status={}, currentStep={}",
                saga.getId(), saga.getStatus(), saga.getCurrentStep());

        try {
             
            if (saga.getStatus() == SagaStatus.PENDING) {
                saga.start();
                paymentTimeoutSagaRepository.save(saga);
            }

            switch (saga.getCurrentStep()) {
                case REOPEN_AUCTION:
                    paymentTimeoutOrchestrator.executeReopenAuctionStep(saga);
                     
                    break;
                case REVERT_BID_STATUS:
                    paymentTimeoutOrchestrator.executeRevertBidStatusStep(saga);
                    break;
                case CANCEL_ORDER:
                    paymentTimeoutOrchestrator.executeCancelOrderStep(saga);
                    break;
                case PUBLISH_REOPEN_EVENT:
                    paymentTimeoutOrchestrator.executePublishReopenEventStep(saga);
                    break;
                default:
                    log.warn("[SagaRecovery] 알 수 없는 단계: sagaId={}, step={}",
                            saga.getId(), saga.getCurrentStep());
            }

            log.info("[SagaRecovery] PaymentTimeoutSaga 재개 성공: sagaId={}", saga.getId());

        } catch (Exception e) {
            log.error("[SagaRecovery] PaymentTimeoutSaga 재개 실패: sagaId={}, error={}",
                    saga.getId(), e.getMessage(), e);

            saga.fail("복구 실패: " + e.getMessage());
            paymentTimeoutSagaRepository.save(saga);

            try {
                paymentTimeoutOrchestrator.compensate(saga.getId(), "RecoveryFailed: " + e.getMessage());
            } catch (Exception compensationError) {
                log.error("[SagaRecovery] 보상 실패: sagaId={}, error={}",
                        saga.getId(), compensationError.getMessage(), compensationError);
            }
        }
    }

    private boolean isPaymentTimeoutZombie(PaymentTimeoutSagaContext saga) {
        LocalDateTime updatedAt = saga.getUpdatedAt();
        if (updatedAt == null) {
            updatedAt = saga.getCreatedAt();
        }

        if (updatedAt == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Duration duration = Duration.between(updatedAt, now);
        long minutesElapsed = duration.toMinutes();

        boolean isZombie = minutesElapsed > zombieTimeoutMinutes;

        if (isZombie) {
            log.warn("[SagaRecovery] Zombie PaymentTimeoutSaga 감지: sagaId={}, status={}, updatedAt={}, minutesElapsed={}",
                    saga.getId(), saga.getStatus(), updatedAt, minutesElapsed);
        }

        return isZombie;
    }

    private void handlePaymentTimeoutZombieSaga(PaymentTimeoutSagaContext saga) {
        log.warn("[SagaRecovery] Zombie PaymentTimeoutSaga 처리 시작: sagaId={}, status={}, currentStep={}",
                saga.getId(), saga.getStatus(), saga.getCurrentStep());

        try {
             
            saga.fail("Zombie 트랜잭션 감지: " + zombieTimeoutMinutes + "분 이상 진행 없음");
            paymentTimeoutSagaRepository.save(saga);

            compensationExecutor.executeCompensations(saga.getId(), "ZombieTransactionDetected");

            log.warn("[SagaRecovery] Zombie PaymentTimeoutSaga 처리 완료: sagaId={}, status=FAILED",
                    saga.getId());

        } catch (Exception e) {
            log.error("[SagaRecovery] Zombie PaymentTimeoutSaga 처리 실패: sagaId={}, error={}",
                    saga.getId(), e.getMessage(), e);
        }
    }

    public void triggerManualRecovery() {
        log.info("[SagaRecovery] 수동 복구 트리거");
        recoverOnStartup();
    }
}
