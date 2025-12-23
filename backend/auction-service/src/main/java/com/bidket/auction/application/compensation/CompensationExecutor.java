package com.bidket.auction.application.compensation;

import com.bidket.auction.domain.compensation.model.CompensationLog;
import com.bidket.auction.domain.compensation.model.CompensationStatus;
import com.bidket.auction.domain.compensation.model.CompensationType;
import com.bidket.auction.domain.compensation.repository.CompensationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompensationExecutor {

    private final CompensationLogRepository compensationLogRepository;
    private final Map<CompensationType, CompensationAction> compensationActions = new HashMap<>();

    public void registerCompensationAction(CompensationType type, CompensationAction action) {
        compensationActions.put(type, action);
        log.info("[CompensationExecutor] 보상 액션 등록: type={}", type);
    }

    @Transactional
    public void executeCompensations(UUID sagaId, String failureReason) {
        log.warn("[CompensationExecutor] 보상 트랜잭션 시작: sagaId={}, reason={}",
                sagaId, failureReason);

        List<CompensationLog> compensationLogs = compensationLogRepository
                .findBySagaIdOrderByStepNumberDesc(sagaId);

        if (compensationLogs.isEmpty()) {
            log.info("[CompensationExecutor] 보상할 항목 없음: sagaId={}", sagaId);
            return;
        }

        log.info("[CompensationExecutor] 보상 대상: sagaId={}, count={}, order=LIFO",
                sagaId, compensationLogs.size());

        int successCount = 0;
        int failureCount = 0;

        for (CompensationLog compensationLog : compensationLogs) {
            try {
                 
                if (compensationLog.getStatus() == CompensationStatus.COMPLETED) {
                    log.info("[CompensationExecutor] 이미 완료된 보상 건너뜀: id={}, type={}",
                            compensationLog.getId(), compensationLog.getCompensationType());
                    successCount++;
                    continue;
                }

                executeCompensation(compensationLog);
                successCount++;

            } catch (Exception e) {
                failureCount++;
                log.error("[CompensationExecutor] 보상 실행 실패: id={}, type={}, error={}",
                        compensationLog.getId(), compensationLog.getCompensationType(),
                        e.getMessage(), e);

                if (compensationLog.isMaxRetriesExceeded()) {
                    compensationLog.fail(e.getMessage());
                    compensationLogRepository.save(compensationLog);
                }
            }
        }

        log.info("[CompensationExecutor] 보상 트랜잭션 완료: sagaId={}, success={}, failure={}",
                sagaId, successCount, failureCount);
    }

    @Transactional
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            retryFor = {Exception.class}
    )
    public void executeCompensation(CompensationLog compensationLog) throws Exception {
        log.info("[CompensationExecutor] 보상 실행 시작: id={}, type={}, retry={}",
                compensationLog.getId(), compensationLog.getCompensationType(),
                compensationLog.getRetryCount());

        compensationLog.startExecution();
        compensationLogRepository.save(compensationLog);

        try {
             
            CompensationAction action = compensationActions.get(compensationLog.getCompensationType());
            if (action == null) {
                throw new IllegalStateException(
                        "보상 액션이 등록되지 않음: " + compensationLog.getCompensationType()
                );
            }

            action.execute(
                    compensationLog.getSagaId(),
                    compensationLog.getAggregateId(),
                    compensationLog.getPayload()
            );

            compensationLog.complete();
            compensationLogRepository.save(compensationLog);

            log.info("[CompensationExecutor] 보상 실행 성공: id={}, type={}",
                    compensationLog.getId(), compensationLog.getCompensationType());

        } catch (Exception e) {
             
            boolean canRetry = compensationLog.incrementRetryCount();
            compensationLogRepository.save(compensationLog);

            log.error("[CompensationExecutor] 보상 실행 실패: id={}, type={}, retry={}/{}, canRetry={}",
                    compensationLog.getId(), compensationLog.getCompensationType(),
                    compensationLog.getRetryCount(), compensationLog.getMaxRetries(),
                    canRetry, e);

            throw e;  
        }
    }

    @Transactional
    public void retryFailedCompensations() {
        log.info("[CompensationExecutor] 실패한 보상 재시도 시작");

        List<CompensationLog> retryableLogs = compensationLogRepository.findRetryableCompensations();

        if (retryableLogs.isEmpty()) {
            log.info("[CompensationExecutor] 재시도 대상 없음");
            return;
        }

        log.info("[CompensationExecutor] 재시도 대상: count={}", retryableLogs.size());

        int successCount = 0;
        int failureCount = 0;

        for (CompensationLog compensationLog : retryableLogs) {
            try {
                executeCompensation(compensationLog);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("[CompensationExecutor] 재시도 실패: id={}, type={}",
                        compensationLog.getId(), compensationLog.getCompensationType(), e);
            }
        }

        log.info("[CompensationExecutor] 재시도 완료: success={}, failure={}",
                successCount, failureCount);
    }

    @Transactional
    public CompensationLog createCompensationLog(UUID sagaId, String sagaType,
                                                  String aggregateType, UUID aggregateId,
                                                  CompensationType compensationType,
                                                  Integer stepNumber, String payload) {
        CompensationLog compensationLog = CompensationLog.builder()
                .sagaId(sagaId)
                .sagaType(sagaType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .compensationType(compensationType)
                .status(CompensationStatus.PENDING)
                .stepNumber(stepNumber)
                .retryCount(0)
                .maxRetries(3)
                .payload(payload)
                .build();

        compensationLog = compensationLogRepository.save(compensationLog);

        log.info("[CompensationExecutor] 보상 로그 생성: id={}, sagaId={}, type={}, step={}",
                compensationLog.getId(), sagaId, compensationType, stepNumber);

        return compensationLog;
    }

    public long countPendingCompensations(UUID sagaId) {
        return compensationLogRepository.countPendingCompensationsBySagaId(sagaId);
    }
}
