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

/**
 * Saga 상태 복구 서비스
 * BACKLOG.md SAGA-004 구현
 *
 * 핵심 기능:
 * 1. 서비스 재시작 시 PENDING/IN_PROGRESS Saga 복구
 * 2. 실패한 OutBox 이벤트 재발행
 * 3. Zombie 트랜잭션 감지 및 처리
 *
 * 장애시나리오.md line 103-155 참조
 */
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

    /**
     * 애플리케이션 시작 시 복구 프로세스 실행
     * ApplicationReadyEvent: 모든 빈이 초기화된 후 발생
     */
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
            // 1. AuctionEndSaga 복구
            recoverAuctionEndSagas();

            // 2. PaymentTimeoutSaga 복구
            recoverPaymentTimeoutSagas();

            // 3. OutBox 이벤트 복구
            recoverOutboxEvents();

            log.info("=== [SagaRecovery] 복구 프로세스 완료 ===");

        } catch (Exception e) {
            log.error("[SagaRecovery] 복구 프로세스 중 오류 발생", e);
            // 복구 실패 시에도 애플리케이션 시작을 막지 않음
        }
    }

    /**
     * AuctionEndSaga 복구
     * PENDING/IN_PROGRESS 상태의 Saga를 재개하거나 보상 처리
     */
    @Transactional
    public void recoverAuctionEndSagas() {
        log.info("[SagaRecovery] AuctionEndSaga 복구 시작");

        // PENDING/IN_PROGRESS 상태의 Saga 조회
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

        // PENDING Saga 처리
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

        // IN_PROGRESS Saga 처리
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

    /**
     * PaymentTimeoutSaga 복구
     * PENDING/IN_PROGRESS 상태의 Saga를 재개하거나 보상 처리
     */
    @Transactional
    public void recoverPaymentTimeoutSagas() {
        log.info("[SagaRecovery] PaymentTimeoutSaga 복구 시작");

        // PENDING/IN_PROGRESS 상태의 Saga 조회
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

        // PENDING Saga 처리
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

        // IN_PROGRESS Saga 처리
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

    /**
     * OutBox 이벤트 복구
     * PENDING/FAILED 상태의 이벤트를 재발행
     */
    @Transactional
    public void recoverOutboxEvents() {
        log.info("[SagaRecovery] OutBox 이벤트 복구 시작");

        // OutBox는 기존 findReadyToPublish 메서드 사용
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
                // 재시도 횟수 체크
                if (outbox.getRetryCount() >= outboxMaxRetries) {
                    log.warn("[SagaRecovery] 최대 재시도 초과: outboxId={}, retryCount={}, maxRetries={}",
                            outbox.getId(), outbox.getRetryCount(), outboxMaxRetries);
                    skippedCount++;
                    continue;
                }

                // 발행 가능 여부 체크
                if (!outbox.canPublish(outboxMaxRetries, now, clock)) {
                    log.debug("[SagaRecovery] 발행 불가 상태: outboxId={}, status={}, retryCount={}",
                            outbox.getId(), outbox.getStatus(), outbox.getRetryCount());
                    skippedCount++;
                    continue;
                }

                // 재발행
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

    /**
     * AuctionEndSaga 재개
     * 마지막 완료된 단계의 다음 단계부터 재개
     *
     * @param saga Saga Context
     */
    private void resumeAuctionEndSaga(AuctionEndSagaContext saga) {
        log.info("[SagaRecovery] AuctionEndSaga 재개: sagaId={}, status={}, currentStep={}",
                saga.getId(), saga.getStatus(), saga.getCurrentStep());

        try {
            // Saga가 PENDING 상태면 시작
            if (saga.getStatus() == SagaStatus.PENDING) {
                saga.start();
                auctionEndSagaRepository.save(saga);
            }

            // 현재 단계부터 재개
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

            // 재개 실패 시 보상 처리
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

    /**
     * Zombie Saga 여부 확인
     * updatedAt이 zombieTimeoutMinutes보다 오래된 경우 zombie로 판단
     *
     * @param saga Saga Context
     * @return zombie 여부
     */
    private boolean isZombie(AuctionEndSagaContext saga) {
        LocalDateTime updatedAt = saga.getUpdatedAt();
        if (updatedAt == null) {
            updatedAt = saga.getCreatedAt();
        }

        if (updatedAt == null) {
            // updatedAt이 null인 경우는 zombie로 간주하지 않음
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

    /**
     * Zombie Saga 처리
     * FAILED로 마킹하고 보상 트랜잭션 실행
     *
     * @param saga Saga Context
     */
    private void handleZombieSaga(AuctionEndSagaContext saga) {
        log.warn("[SagaRecovery] Zombie Saga 처리 시작: sagaId={}, status={}, currentStep={}",
                saga.getId(), saga.getStatus(), saga.getCurrentStep());

        try {
            // Saga를 FAILED로 마킹
            saga.fail("Zombie 트랜잭션 감지: " + zombieTimeoutMinutes + "분 이상 진행 없음");
            auctionEndSagaRepository.save(saga);

            // 보상 트랜잭션 실행
            compensationExecutor.executeCompensations(saga.getId(), "ZombieTransactionDetected");

            log.warn("[SagaRecovery] Zombie Saga 처리 완료: sagaId={}, status=FAILED",
                    saga.getId());

        } catch (Exception e) {
            log.error("[SagaRecovery] Zombie Saga 처리 실패: sagaId={}, error={}",
                    saga.getId(), e.getMessage(), e);
        }
    }

    /**
     * PaymentTimeoutSaga 재개
     * 마지막 완료된 단계의 다음 단계부터 재개
     *
     * @param saga PaymentTimeoutSaga Context
     */
    private void resumePaymentTimeoutSaga(PaymentTimeoutSagaContext saga) {
        log.info("[SagaRecovery] PaymentTimeoutSaga 재개: sagaId={}, status={}, currentStep={}",
                saga.getId(), saga.getStatus(), saga.getCurrentStep());

        try {
            // Saga가 PENDING 상태면 시작
            if (saga.getStatus() == SagaStatus.PENDING) {
                saga.start();
                paymentTimeoutSagaRepository.save(saga);
            }

            // 현재 단계부터 재개
            switch (saga.getCurrentStep()) {
                case REOPEN_AUCTION:
                    paymentTimeoutOrchestrator.executeReopenAuctionStep(saga);
                    // 다음 단계 자동 실행 (orchestrator 내부에서 처리)
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

            // 재개 실패 시 보상 처리
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

    /**
     * PaymentTimeoutSaga Zombie 여부 확인
     *
     * @param saga PaymentTimeoutSaga Context
     * @return zombie 여부
     */
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

    /**
     * PaymentTimeoutSaga Zombie 처리
     *
     * @param saga PaymentTimeoutSaga Context
     */
    private void handlePaymentTimeoutZombieSaga(PaymentTimeoutSagaContext saga) {
        log.warn("[SagaRecovery] Zombie PaymentTimeoutSaga 처리 시작: sagaId={}, status={}, currentStep={}",
                saga.getId(), saga.getStatus(), saga.getCurrentStep());

        try {
            // Saga를 FAILED로 마킹
            saga.fail("Zombie 트랜잭션 감지: " + zombieTimeoutMinutes + "분 이상 진행 없음");
            paymentTimeoutSagaRepository.save(saga);

            // 보상 트랜잭션 실행
            compensationExecutor.executeCompensations(saga.getId(), "ZombieTransactionDetected");

            log.warn("[SagaRecovery] Zombie PaymentTimeoutSaga 처리 완료: sagaId={}, status=FAILED",
                    saga.getId());

        } catch (Exception e) {
            log.error("[SagaRecovery] Zombie PaymentTimeoutSaga 처리 실패: sagaId={}, error={}",
                    saga.getId(), e.getMessage(), e);
        }
    }

    /**
     * 수동 복구 트리거 (관리자용)
     * 운영 중 필요 시 수동으로 복구 프로세스 실행
     */
    public void triggerManualRecovery() {
        log.info("[SagaRecovery] 수동 복구 트리거");
        recoverOnStartup();
    }
}
