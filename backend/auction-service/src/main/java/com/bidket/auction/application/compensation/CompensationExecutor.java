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

/**
 * 보상 트랜잭션 실행자
 * BACKLOG.md SAGA-003 (line 581-583) 구현
 *
 * 핵심 기능:
 * 1. 역순 보상 실행 (LIFO - Last In First Out)
 * 2. 재시도 3회 (exponential backoff)
 * 3. compensation_log 기반 추적
 * 4. Idempotency 보장
 *
 * 장애시나리오.md line 95-101 참조
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompensationExecutor {

    private final CompensationLogRepository compensationLogRepository;
    private final Map<CompensationType, CompensationAction> compensationActions = new HashMap<>();

    /**
     * 보상 액션 등록
     *
     * @param type   보상 타입
     * @param action 보상 액션
     */
    public void registerCompensationAction(CompensationType type, CompensationAction action) {
        compensationActions.put(type, action);
        log.info("[CompensationExecutor] 보상 액션 등록: type={}", type);
    }

    /**
     * Saga의 모든 보상 실행 (역순)
     * 장애시나리오.md line 29 참조
     *
     * @param sagaId        Saga ID
     * @param failureReason 실패 사유
     */
    @Transactional
    public void executeCompensations(UUID sagaId, String failureReason) {
        log.warn("[CompensationExecutor] 보상 트랜잭션 시작: sagaId={}, reason={}",
                sagaId, failureReason);

        // 1. Saga ID로 보상 로그 조회 (stepNumber 역순)
        List<CompensationLog> compensationLogs = compensationLogRepository
                .findBySagaIdOrderByStepNumberDesc(sagaId);

        if (compensationLogs.isEmpty()) {
            log.info("[CompensationExecutor] 보상할 항목 없음: sagaId={}", sagaId);
            return;
        }

        log.info("[CompensationExecutor] 보상 대상: sagaId={}, count={}, order=LIFO",
                sagaId, compensationLogs.size());

        // 2. 역순으로 보상 실행 (LIFO)
        int successCount = 0;
        int failureCount = 0;

        for (CompensationLog compensationLog : compensationLogs) {
            try {
                // Idempotency: 이미 완료된 보상은 건너뜀
                if (compensationLog.getStatus() == CompensationStatus.COMPLETED) {
                    log.info("[CompensationExecutor] 이미 완료된 보상 건너뜀: id={}, type={}",
                            compensationLog.getId(), compensationLog.getCompensationType());
                    successCount++;
                    continue;
                }

                // 보상 실행
                executeCompensation(compensationLog);
                successCount++;

            } catch (Exception e) {
                failureCount++;
                log.error("[CompensationExecutor] 보상 실행 실패: id={}, type={}, error={}",
                        compensationLog.getId(), compensationLog.getCompensationType(),
                        e.getMessage(), e);

                // 최대 재시도 초과 시 FAILED로 기록
                if (compensationLog.isMaxRetriesExceeded()) {
                    compensationLog.fail(e.getMessage());
                    compensationLogRepository.save(compensationLog);
                }
            }
        }

        log.info("[CompensationExecutor] 보상 트랜잭션 완료: sagaId={}, success={}, failure={}",
                sagaId, successCount, failureCount);
    }

    /**
     * 단일 보상 실행 (재시도 포함)
     *
     * @param compensationLog 보상 로그
     * @throws Exception 보상 실행 실패 시
     */
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

        // 1. 상태 업데이트: IN_PROGRESS
        compensationLog.startExecution();
        compensationLogRepository.save(compensationLog);

        try {
            // 2. 보상 액션 조회
            CompensationAction action = compensationActions.get(compensationLog.getCompensationType());
            if (action == null) {
                throw new IllegalStateException(
                        "보상 액션이 등록되지 않음: " + compensationLog.getCompensationType()
                );
            }

            // 3. 보상 실행
            action.execute(
                    compensationLog.getSagaId(),
                    compensationLog.getAggregateId(),
                    compensationLog.getPayload()
            );

            // 4. 상태 업데이트: COMPLETED
            compensationLog.complete();
            compensationLogRepository.save(compensationLog);

            log.info("[CompensationExecutor] 보상 실행 성공: id={}, type={}",
                    compensationLog.getId(), compensationLog.getCompensationType());

        } catch (Exception e) {
            // 5. 재시도 횟수 증가
            boolean canRetry = compensationLog.incrementRetryCount();
            compensationLogRepository.save(compensationLog);

            log.error("[CompensationExecutor] 보상 실행 실패: id={}, type={}, retry={}/{}, canRetry={}",
                    compensationLog.getId(), compensationLog.getCompensationType(),
                    compensationLog.getRetryCount(), compensationLog.getMaxRetries(),
                    canRetry, e);

            throw e; // 재시도를 위해 예외 재발생
        }
    }

    /**
     * 재시도 가능한 보상 실행 (배치)
     * Scheduler에서 주기적으로 호출
     */
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

    /**
     * 보상 로그 생성 헬퍼 메서드
     *
     * @param sagaId           Saga ID
     * @param sagaType         Saga 타입
     * @param aggregateType    집계 타입
     * @param aggregateId      집계 ID
     * @param compensationType 보상 타입
     * @param stepNumber       단계 번호
     * @param payload          페이로드
     * @return 생성된 보상 로그
     */
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

    /**
     * Saga의 미완료 보상 개수 확인
     *
     * @param sagaId Saga ID
     * @return 미완료 보상 개수
     */
    public long countPendingCompensations(UUID sagaId) {
        return compensationLogRepository.countPendingCompensationsBySagaId(sagaId);
    }
}
