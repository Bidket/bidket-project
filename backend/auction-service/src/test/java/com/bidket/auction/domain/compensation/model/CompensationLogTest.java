package com.bidket.auction.domain.compensation.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CompensationLog 엔티티 테스트
 */
@DisplayName("CompensationLog 엔티티 테스트")
class CompensationLogTest {

    @Test
    @DisplayName("보상 로그 생성 성공")
    void createCompensationLog_Success() {
        // given
        UUID sagaId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        // when
        CompensationLog log = CompensationLog.builder()
                .sagaId(sagaId)
                .sagaType("AUCTION_END_SAGA")
                .aggregateType("AUCTION")
                .aggregateId(aggregateId)
                .compensationType(CompensationType.REOPEN_AUCTION)
                .stepNumber(3)
                .payload("{\"auctionId\":\"test\"}")
                .build();

        // then
        assertThat(log).isNotNull();
        assertThat(log.getSagaId()).isEqualTo(sagaId);
        assertThat(log.getStatus()).isEqualTo(CompensationStatus.PENDING);
        assertThat(log.getRetryCount()).isEqualTo(0);
        assertThat(log.getMaxRetries()).isEqualTo(3);
    }

    @Test
    @DisplayName("보상 실행 시작 - PENDING에서 IN_PROGRESS로 전환")
    void startExecution_FromPending_Success() {
        // given
        CompensationLog log = createTestLog();

        // when
        log.startExecution();

        // then
        assertThat(log.getStatus()).isEqualTo(CompensationStatus.IN_PROGRESS);
        assertThat(log.getExecutedAt()).isNotNull();
    }

    @Test
    @DisplayName("보상 실행 시작 - FAILED에서도 가능")
    void startExecution_FromFailed_Success() {
        // given
        CompensationLog log = createTestLog();
        log.fail("이전 실패");

        // when
        log.startExecution();

        // then
        assertThat(log.getStatus()).isEqualTo(CompensationStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("보상 실행 시작 - IN_PROGRESS 또는 COMPLETED에서 실패")
    void startExecution_FromInvalidStatus_ThrowsException() {
        // given
        CompensationLog log = createTestLog();
        log.startExecution(); // IN_PROGRESS

        // when & then
        assertThatThrownBy(() -> log.startExecution())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot start execution");
    }

    @Test
    @DisplayName("보상 완료 - IN_PROGRESS에서 COMPLETED로 전환")
    void complete_FromInProgress_Success() {
        // given
        CompensationLog log = createTestLog();
        log.startExecution();

        // when
        log.complete();

        // then
        assertThat(log.getStatus()).isEqualTo(CompensationStatus.COMPLETED);
        assertThat(log.getCompletedAt()).isNotNull();
        assertThat(log.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("보상 완료 - IN_PROGRESS 아닌 상태에서 실패")
    void complete_FromInvalidStatus_ThrowsException() {
        // given
        CompensationLog log = createTestLog();

        // when & then
        assertThatThrownBy(() -> log.complete())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot complete");
    }

    @Test
    @DisplayName("보상 실패 처리")
    void fail_Success() {
        // given
        CompensationLog log = createTestLog();
        log.startExecution();

        // when
        log.fail("네트워크 오류");

        // then
        assertThat(log.getStatus()).isEqualTo(CompensationStatus.FAILED);
        assertThat(log.getErrorMessage()).isEqualTo("네트워크 오류");
        assertThat(log.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("재시도 횟수 증가 - 최대 재시도 미만")
    void incrementRetryCount_BelowMax_ReturnsTrue() {
        // given
        CompensationLog log = createTestLog();

        // when
        boolean canRetry = log.incrementRetryCount();

        // then
        assertThat(log.getRetryCount()).isEqualTo(1);
        assertThat(canRetry).isTrue();
    }

    @Test
    @DisplayName("재시도 횟수 증가 - 최대 재시도 도달")
    void incrementRetryCount_ReachedMax_ReturnsFalse() {
        // given
        CompensationLog log = CompensationLog.builder()
                .sagaId(UUID.randomUUID())
                .sagaType("TEST")
                .aggregateType("AUCTION")
                .aggregateId(UUID.randomUUID())
                .compensationType(CompensationType.REOPEN_AUCTION)
                .stepNumber(1)
                .retryCount(2) // maxRetries = 3
                .maxRetries(3)
                .payload("{}")
                .build();

        // when
        boolean canRetry = log.incrementRetryCount();

        // then
        assertThat(log.getRetryCount()).isEqualTo(3);
        assertThat(canRetry).isFalse(); // 3회 도달
    }

    @Test
    @DisplayName("재시도 가능 여부 - PENDING 상태이고 최대 재시도 미만")
    void canRetry_PendingAndBelowMax_ReturnsTrue() {
        // given
        CompensationLog log = createTestLog();

        // when
        boolean canRetry = log.canRetry();

        // then
        assertThat(canRetry).isTrue();
    }

    @Test
    @DisplayName("재시도 가능 여부 - FAILED 상태이고 최대 재시도 미만")
    void canRetry_FailedAndBelowMax_ReturnsTrue() {
        // given
        CompensationLog log = createTestLog();
        log.fail("오류");

        // when
        boolean canRetry = log.canRetry();

        // then
        assertThat(canRetry).isTrue();
    }

    @Test
    @DisplayName("재시도 가능 여부 - COMPLETED 상태")
    void canRetry_Completed_ReturnsFalse() {
        // given
        CompensationLog log = createTestLog();
        log.startExecution();
        log.complete();

        // when
        boolean canRetry = log.canRetry();

        // then
        assertThat(canRetry).isFalse();
    }

    @Test
    @DisplayName("재시도 가능 여부 - 최대 재시도 초과")
    void canRetry_MaxRetriesExceeded_ReturnsFalse() {
        // given
        CompensationLog log = CompensationLog.builder()
                .sagaId(UUID.randomUUID())
                .sagaType("TEST")
                .aggregateType("AUCTION")
                .aggregateId(UUID.randomUUID())
                .compensationType(CompensationType.REOPEN_AUCTION)
                .stepNumber(1)
                .retryCount(3)
                .maxRetries(3)
                .payload("{}")
                .build();

        // when
        boolean canRetry = log.canRetry();

        // then
        assertThat(canRetry).isFalse();
    }

    @Test
    @DisplayName("최대 재시도 초과 여부 확인")
    void isMaxRetriesExceeded_True() {
        // given
        CompensationLog log = CompensationLog.builder()
                .sagaId(UUID.randomUUID())
                .sagaType("TEST")
                .aggregateType("AUCTION")
                .aggregateId(UUID.randomUUID())
                .compensationType(CompensationType.REOPEN_AUCTION)
                .stepNumber(1)
                .retryCount(3)
                .maxRetries(3)
                .payload("{}")
                .build();

        // when
        boolean isExceeded = log.isMaxRetriesExceeded();

        // then
        assertThat(isExceeded).isTrue();
    }

    // ===== Helper Methods =====

    private CompensationLog createTestLog() {
        return CompensationLog.builder()
                .sagaId(UUID.randomUUID())
                .sagaType("AUCTION_END_SAGA")
                .aggregateType("AUCTION")
                .aggregateId(UUID.randomUUID())
                .compensationType(CompensationType.REOPEN_AUCTION)
                .stepNumber(1)
                .payload("{}")
                .build();
    }
}
