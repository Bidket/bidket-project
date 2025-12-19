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
 * [Senior's Guide: 역할]
 * 이 클래스는 Saga 패턴의 "보상 트랜잭션 실행자"로, 분산 트랜잭션이 실패했을 때
 * 이미 실행된 단계들을 안전하게 되돌리는 역할을 담당합니다.
 *
 * [도메인 흐름에서의 위치]
 * Saga 실행 흐름: [Step 1 → Step 2 → Step 3 → 실패!]
 * 보상 실행 흐름: [Step 3 보상 → Step 2 보상 → Step 1 보상] (역순)
 *
 * 예시) 경매 종료 Saga가 Step 2에서 실패한 경우:
 * 1. AuctionEndSagaOrchestrator가 실패를 감지
 * 2. CompensationExecutor.executeCompensations() 호출
 * 3. Step 1 보상 실행 (주문 취소)
 * 4. Saga 상태를 COMPENSATED로 변경
 *
 * [왜 이렇게 설계했을까?]
 *
 * 1. @Service
 *    - 스프링 컨테이너가 싱글톤으로 관리하여 모든 Saga에서 공유
 *    - 보상 로직의 중앙 집중화로 일관성 보장
 *
 * 2. @RequiredArgsConstructor (Lombok)
 *    - final 필드(compensationLogRepository)를 생성자 주입
 *    - 불변성 보장으로 스레드 안전성 확보
 *
 * 3. Map<CompensationType, CompensationAction> compensationActions
 *    - 전략 패턴(Strategy Pattern) 적용
 *    - 각 보상 타입(CANCEL_ORDER, REVERT_BID_STATUS 등)마다 다른 액션 클래스를 등록
 *    - 런타임에 보상 타입에 따라 적절한 액션을 선택하여 실행
 *    - 새로운 보상 타입 추가 시 기존 코드 수정 없이 확장 가능 (Open/Closed Principle)
 *
 * 4. @Transactional
 *    - 보상 실행과 로그 상태 업데이트를 하나의 트랜잭션으로 묶음
 *    - 보상 실행 중 예외 발생 시 롤백되어 데이터 일관성 보장
 *
 * 5. @Retryable (Spring Retry)
 *    - maxAttempts = 3: 최대 3번까지 재시도
 *    - backoff = @Backoff(delay = 1000, multiplier = 2.0)
 *      * 첫 번째 재시도: 1초 후
 *      * 두 번째 재시도: 2초 후 (1 × 2.0)
 *      * 세 번째 재시도: 4초 후 (2 × 2.0)
 *    - 일시적인 네트워크 오류, DB 락 대기 등을 자동으로 복구
 *
 * 6. LIFO (Last-In-First-Out) 실행 순서
 *    - Saga의 Step들은 서로 의존 관계를 가짐
 *      예) Step 1: 주문 생성 → Step 2: 입찰 낙찰 처리
 *    - Step 1 보상(주문 취소)을 먼저 하면 Step 2 보상이 실패할 수 있음
 *    - 따라서 Step 2 보상 → Step 1 보상 순서로 역순 실행
 *
 * 7. Idempotency (멱등성) 보장
 *    - 이미 COMPLETED 상태인 보상은 건너뜀 (line 81-86)
 *    - 서비스 재시작, 네트워크 타임아웃 등으로 중복 호출되어도 안전
 *    - 예) 보상 Step 1 완료 → 서버 재시작 → Step 1은 건너뛰고 Step 2부터 재개
 *
 * [핵심 메서드 설명]
 *
 * 1. registerCompensationAction(type, action)
 *    - CompensationConfig에서 애플리케이션 시작 시 호출
 *    - 각 보상 타입에 대응하는 액션 클래스를 Map에 등록
 *    - 예: CANCEL_ORDER → CancelOrderCompensationAction
 *
 * 2. executeCompensations(sagaId, failureReason)
 *    - Saga 실패 시 모든 보상을 역순으로 실행하는 메인 메서드
 *    - 실행 흐름:
 *      1) sagaId로 보상 로그 조회 (stepNumber 역순 정렬)
 *      2) 각 보상 로그에 대해:
 *         - 이미 COMPLETED면 건너뜀 (Idempotency)
 *         - executeCompensation() 호출
 *      3) 성공/실패 개수 집계 및 로깅
 *
 * 3. executeCompensation(compensationLog)
 *    - 단일 보상을 실행하는 메서드
 *    - 실행 흐름:
 *      1) 상태를 IN_PROGRESS로 변경 (진행 중 표시)
 *      2) Map에서 보상 타입에 맞는 액션 조회
 *      3) action.execute() 호출 (실제 보상 로직)
 *      4) 성공 시 COMPLETED로 변경
 *      5) 실패 시 retryCount 증가 및 예외 재발생
 *    - @Retryable에 의해 자동 재시도 (최대 3회)
 *
 * 4. retryFailedCompensations()
 *    - 스케줄러가 주기적으로 호출 (예: 1분마다)
 *    - 과거에 실패했지만 재시도 가능한 보상들을 다시 시도
 *    - findRetryableCompensations(): status=IN_PROGRESS이면서 maxRetries 미만
 *
 * 5. createCompensationLog(...)
 *    - Saga Orchestrator가 각 Step 실행 전에 호출
 *    - 보상이 필요한 단계마다 로그를 미리 생성
 *    - 예) AuctionEndSaga Step 1 실행 전:
 *      createCompensationLog(sagaId, "AUCTION_END", "ORDER", orderId,
 *                            CANCEL_ORDER, 1, "{orderId:...}")
 *
 * [어디서 이 클래스를 사용하나요?]
 * - {@link com.bidket.auction.application.saga.AuctionEndSagaOrchestrator}: 경매 종료 Saga 보상
 * - {@link com.bidket.auction.application.saga.PaymentTimeoutSagaOrchestrator}: 결제 타임아웃 Saga 보상
 * - {@link com.bidket.auction.application.saga.recovery.SagaRecoveryService}: Zombie Saga 복구
 * - {@link com.bidket.auction.application.compensation.actions.CancelOrderCompensationAction}: 주문 취소 액션
 * - {@link com.bidket.auction.application.compensation.actions.RevertBidStatusCompensationAction}: 입찰 상태 복원 액션
 * - {@link com.bidket.auction.application.compensation.actions.ReopenAuctionCompensationAction}: 경매 재개 액션
 * - {@link com.bidket.auction.application.compensation.actions.CancelPaymentCompensationAction}: 결제 취소 액션
 * - {@link com.bidket.auction.infrastructure.config.CompensationConfig}: 보상 액션 등록
 *
 * [신입 개발자 주의사항]
 * 1. 보상 로직은 반드시 멱등성(Idempotency)을 보장해야 합니다
 *    - 같은 보상을 여러 번 실행해도 결과가 동일해야 함
 *    - 예) "주문 취소"를 2번 호출해도 안전해야 함
 *
 * 2. 보상 액션 등록을 빠뜨리면 런타임 에러 발생
 *    - CompensationConfig에서 새 보상 타입 추가 시 반드시 registerCompensationAction() 호출
 *    - 등록 안 하면 "보상 액션이 등록되지 않음" 예외 발생 (line 135-137)
 *
 * 3. 보상 순서가 중요합니다 (LIFO)
 *    - stepNumber를 정확히 설정하세요
 *    - 예) Step 1이 Step 2에 의존한다면, Step 2 보상을 먼저 실행해야 함
 *
 * 4. 보상 로그의 maxRetries는 신중히 설정
 *    - 기본값 3회는 일시적 오류(네트워크, DB 락)를 위한 것
 *    - 영구적 오류(데이터 불일치)는 재시도해도 실패하므로 알람 필요
 *
 * 5. 보상 실패 시 수동 개입이 필요할 수 있습니다
 *    - 3회 재시도 후에도 실패하면 상태가 FAILED로 남음
 *    - 운영팀이 compensation_log 테이블을 모니터링해야 함
 *
 * BACKLOG.md SAGA-003 (line 581-583) 구현
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
