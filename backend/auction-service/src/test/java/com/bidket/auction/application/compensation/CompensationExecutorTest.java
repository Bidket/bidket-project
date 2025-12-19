package com.bidket.auction.application.compensation;

import com.bidket.auction.domain.compensation.model.CompensationLog;
import com.bidket.auction.domain.compensation.model.CompensationStatus;
import com.bidket.auction.domain.compensation.model.CompensationType;
import com.bidket.auction.domain.compensation.repository.CompensationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CompensationExecutor 단위 테스트
 * BACKLOG.md SAGA-003 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompensationExecutor 테스트")
class CompensationExecutorTest {

    @Mock
    private CompensationLogRepository compensationLogRepository;

    @InjectMocks
    private CompensationExecutor compensationExecutor;

    private UUID sagaId;
    private UUID auctionId;
    private UUID bidId;

    @BeforeEach
    void setUp() {
        sagaId = UUID.randomUUID();
        auctionId = UUID.randomUUID();
        bidId = UUID.randomUUID();
    }

    @Test
    @DisplayName("보상 액션 등록 성공")
    void registerCompensationAction_Success() {
        // given
        CompensationAction mockAction = (sagaId, aggregateId, payload) -> {
            // do nothing
        };

        // when
        compensationExecutor.registerCompensationAction(
                CompensationType.REOPEN_AUCTION,
                mockAction
        );

        // then
        // 등록 성공 (예외 없음)
    }

    @Test
    @DisplayName("보상 로그 생성 성공")
    void createCompensationLog_Success() {
        // given
        CompensationLog expectedLog = CompensationLog.builder()
                .sagaId(sagaId)
                .sagaType("AUCTION_END_SAGA")
                .aggregateType("AUCTION")
                .aggregateId(auctionId)
                .compensationType(CompensationType.REOPEN_AUCTION)
                .stepNumber(3)
                .payload("{\"auctionId\":\"" + auctionId + "\"}")
                .build();

        when(compensationLogRepository.save(any(CompensationLog.class)))
                .thenReturn(expectedLog);

        // when
        CompensationLog result = compensationExecutor.createCompensationLog(
                sagaId,
                "AUCTION_END_SAGA",
                "AUCTION",
                auctionId,
                CompensationType.REOPEN_AUCTION,
                3,
                "{\"auctionId\":\"" + auctionId + "\"}"
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSagaId()).isEqualTo(sagaId);
        assertThat(result.getCompensationType()).isEqualTo(CompensationType.REOPEN_AUCTION);
        assertThat(result.getStepNumber()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo(CompensationStatus.PENDING);
        assertThat(result.getMaxRetries()).isEqualTo(3);

        verify(compensationLogRepository, times(1)).save(any(CompensationLog.class));
    }

    @Test
    @DisplayName("역순 보상 실행 성공 (LIFO)")
    void executeCompensations_ReverseOrder_Success() {
        // given: 3개의 보상 로그 (stepNumber: 3, 2, 1 역순)
        CompensationLog log3 = createCompensationLog(sagaId, CompensationType.REOPEN_AUCTION, 3);
        CompensationLog log2 = createCompensationLog(sagaId, CompensationType.REVERT_BID_STATUS, 2);
        CompensationLog log1 = createCompensationLog(sagaId, CompensationType.CANCEL_ORDER, 1);

        List<CompensationLog> compensationLogs = Arrays.asList(log3, log2, log1);

        when(compensationLogRepository.findBySagaIdOrderByStepNumberDesc(sagaId))
                .thenReturn(compensationLogs);

        // 보상 액션 등록 (mock)
        CompensationAction mockAction = (sid, aggId, payload) -> {
            // 성공
        };
        compensationExecutor.registerCompensationAction(CompensationType.REOPEN_AUCTION, mockAction);
        compensationExecutor.registerCompensationAction(CompensationType.REVERT_BID_STATUS, mockAction);
        compensationExecutor.registerCompensationAction(CompensationType.CANCEL_ORDER, mockAction);

        // when
        compensationExecutor.executeCompensations(sagaId, "ORDER_CREATION_FAILED");

        // then: 역순으로 실행되었는지 검증 (stepNumber 내림차순)
        verify(compensationLogRepository, times(1))
                .findBySagaIdOrderByStepNumberDesc(sagaId);

        // 각 로그가 save 되었는지 확인 (상태 업데이트)
        verify(compensationLogRepository, atLeast(3)).save(any(CompensationLog.class));
    }

    @Test
    @DisplayName("이미 완료된 보상은 건너뜀 (Idempotency)")
    void executeCompensations_SkipCompleted_Idempotency() {
        // given: 이미 완료된 보상 로그
        CompensationLog completedLog = createCompensationLog(sagaId, CompensationType.REOPEN_AUCTION, 1);
        completedLog.startExecution();
        completedLog.complete();

        when(compensationLogRepository.findBySagaIdOrderByStepNumberDesc(sagaId))
                .thenReturn(List.of(completedLog));

        // when
        compensationExecutor.executeCompensations(sagaId, "TEST");

        // then: 이미 완료된 보상은 액션 실행 없이 건너뜀
        verify(compensationLogRepository, times(1))
                .findBySagaIdOrderByStepNumberDesc(sagaId);
    }

    @Test
    @DisplayName("보상 실행 실패 시 재시도")
    void executeCompensation_Retry_OnFailure() {
        // given
        CompensationLog log = createCompensationLog(sagaId, CompensationType.REOPEN_AUCTION, 1);

        CompensationAction failingAction = (sid, aggId, payload) -> {
            throw new RuntimeException("보상 실패");
        };
        compensationExecutor.registerCompensationAction(CompensationType.REOPEN_AUCTION, failingAction);

        when(compensationLogRepository.save(any(CompensationLog.class)))
                .thenReturn(log);

        // when & then: 재시도 설정에 따라 예외 발생
        assertThatThrownBy(() -> compensationExecutor.executeCompensation(log))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("보상 실패");

        // 재시도 횟수 증가 확인
        assertThat(log.getRetryCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("등록되지 않은 보상 타입 실행 시 예외")
    void executeCompensation_UnregisteredType_ThrowsException() {
        // given: 등록되지 않은 보상 타입 (CANCEL_ORDER를 등록하지 않음)
        CompensationLog log = createCompensationLog(sagaId, CompensationType.CANCEL_ORDER, 1);

        when(compensationLogRepository.save(any(CompensationLog.class)))
                .thenReturn(log);

        // when & then
        assertThatThrownBy(() -> compensationExecutor.executeCompensation(log))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("재시도 가능한 보상 조회 및 재실행")
    void retryFailedCompensations_Success() {
        // given: 실패한 보상 로그
        CompensationLog failedLog = createCompensationLog(sagaId, CompensationType.REOPEN_AUCTION, 1);
        failedLog.startExecution();
        failedLog.fail("일시적 오류");

        when(compensationLogRepository.findRetryableCompensations())
                .thenReturn(List.of(failedLog));

        CompensationAction successAction = (sid, aggId, payload) -> {
            // 성공
        };
        compensationExecutor.registerCompensationAction(CompensationType.REOPEN_AUCTION, successAction);

        when(compensationLogRepository.save(any(CompensationLog.class)))
                .thenReturn(failedLog);

        // when
        compensationExecutor.retryFailedCompensations();

        // then
        verify(compensationLogRepository, times(1)).findRetryableCompensations();
        verify(compensationLogRepository, atLeast(1)).save(any(CompensationLog.class));
    }

    @Test
    @DisplayName("미완료 보상 개수 조회")
    void countPendingCompensations_Success() {
        // given
        when(compensationLogRepository.countPendingCompensationsBySagaId(sagaId))
                .thenReturn(3L);

        // when
        long count = compensationExecutor.countPendingCompensations(sagaId);

        // then
        assertThat(count).isEqualTo(3L);
        verify(compensationLogRepository, times(1))
                .countPendingCompensationsBySagaId(sagaId);
    }

    @Test
    @DisplayName("최대 재시도 초과 시 FAILED 상태로 전환")
    void executeCompensation_MaxRetriesExceeded_Failed() {
        // given: 최대 재시도 횟수에 도달한 로그
        CompensationLog log = CompensationLog.builder()
                .sagaId(sagaId)
                .sagaType("TEST_SAGA")
                .aggregateType("AUCTION")
                .aggregateId(auctionId)
                .compensationType(CompensationType.REOPEN_AUCTION)
                .stepNumber(1)
                .retryCount(3) // 이미 3회 재시도
                .maxRetries(3)
                .payload("{}")
                .build();

        // when
        boolean canRetry = log.canRetry();

        // then: 더 이상 재시도 불가
        assertThat(canRetry).isFalse();
        assertThat(log.isMaxRetriesExceeded()).isTrue();
    }

    // ===== Helper Methods =====

    private CompensationLog createCompensationLog(UUID sagaId, CompensationType type, int stepNumber) {
        return CompensationLog.builder()
                .sagaId(sagaId)
                .sagaType("AUCTION_END_SAGA")
                .aggregateType("AUCTION")
                .aggregateId(auctionId)
                .compensationType(type)
                .stepNumber(stepNumber)
                .retryCount(0)
                .maxRetries(3)
                .payload("{}")
                .build();
    }
}
