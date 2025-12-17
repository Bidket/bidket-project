package com.bidket.auction.application.saga;

import com.bidket.auction.application.compensation.CompensationExecutor;
import com.bidket.auction.application.outbox.publisher.OutboxEventPublisher;
import com.bidket.auction.domain.outbox.model.AuctionOutbox;
import com.bidket.auction.domain.outbox.model.OutboxStatus;
import com.bidket.auction.domain.outbox.repository.OutboxRepository;
import com.bidket.auction.domain.saga.model.AuctionEndSagaContext;
import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaContext;
import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaStep;
import com.bidket.auction.domain.saga.model.SagaStatus;
import com.bidket.auction.domain.saga.model.SagaStep;
import com.bidket.auction.domain.saga.repository.AuctionEndSagaContextRepository;
import com.bidket.auction.domain.saga.repository.PaymentTimeoutSagaContextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SagaRecoveryService 단위 테스트
 * SAGA-004 복구 로직 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SagaRecoveryService 단위 테스트")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class SagaRecoveryServiceTest {

    @Mock
    private AuctionEndSagaContextRepository auctionEndSagaRepository;

    @Mock
    private PaymentTimeoutSagaContextRepository paymentTimeoutSagaRepository;

    @Mock
    private AuctionEndSagaOrchestrator auctionEndOrchestrator;

    @Mock
    private PaymentTimeoutSagaOrchestrator paymentTimeoutOrchestrator;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxEventPublisher outboxPublisher;

    @Mock
    private CompensationExecutor compensationExecutor;

    @Mock
    private Clock clock;

    @InjectMocks
    private SagaRecoveryService sagaRecoveryService;

    private final ZoneId zone = ZoneId.of("Asia/Seoul");
    private LocalDateTime now;
    private Instant nowInstant;

    @BeforeEach
    void setUp() {
        // 현재 시간 설정
        now = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        nowInstant = now.atZone(zone).toInstant();

        when(clock.instant()).thenReturn(nowInstant);
        when(clock.getZone()).thenReturn(zone);

        // 설정값 주입
        ReflectionTestUtils.setField(sagaRecoveryService, "recoveryEnabled", true);
        ReflectionTestUtils.setField(sagaRecoveryService, "zombieTimeoutMinutes", 10);
        ReflectionTestUtils.setField(sagaRecoveryService, "outboxMaxRetries", 3);
        ReflectionTestUtils.setField(sagaRecoveryService, "outboxBatchSize", 100);
    }

    @Test
    @DisplayName("복구 기능이 비활성화되면 복구를 수행하지 않는다")
    void recoverOnStartup_WhenDisabled_ShouldSkipRecovery() {
        // given
        ReflectionTestUtils.setField(sagaRecoveryService, "recoveryEnabled", false);

        // when
        sagaRecoveryService.recoverOnStartup();

        // then
        verifyNoInteractions(auctionEndSagaRepository);
        verifyNoInteractions(paymentTimeoutSagaRepository);
        verifyNoInteractions(outboxRepository);
    }

    @Test
    @DisplayName("PENDING 상태의 AuctionEndSaga를 재개한다")
    void recoverAuctionEndSagas_WithPendingSaga_ShouldResume() {
        // given
        UUID sagaId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        AuctionEndSagaContext originalSaga = createAuctionEndSaga(sagaId, auctionId, SagaStatus.PENDING, SagaStep.CREATE_ORDER);
        AuctionEndSagaContext saga = spy(originalSaga);

        when(auctionEndSagaRepository.findByStatus(SagaStatus.PENDING)).thenReturn(List.of(saga));

        // when
        sagaRecoveryService.recoverAuctionEndSagas();

        // then
        verify(saga).start();
        verify(auctionEndSagaRepository, atLeastOnce()).save(saga);
        verify(auctionEndOrchestrator).executeCreateOrderStep(saga);
        verifyNoInteractions(compensationExecutor);
    }

    @Test
    @DisplayName("IN_PROGRESS 상태의 AuctionEndSaga를 현재 단계부터 재개한다")
    void recoverAuctionEndSagas_WithInProgressSaga_ShouldResumeFromCurrentStep() {
        // given
        UUID sagaId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        AuctionEndSagaContext saga = createAuctionEndSaga(sagaId, auctionId, SagaStatus.IN_PROGRESS, SagaStep.MARK_WINNING_BID);

        when(auctionEndSagaRepository.findByStatus(SagaStatus.IN_PROGRESS)).thenReturn(List.of(saga));

        // when
        sagaRecoveryService.recoverAuctionEndSagas();

        // then
        verify(auctionEndOrchestrator).executeMarkWinningBidStep(saga);
        verify(auctionEndOrchestrator, never()).executeCreateOrderStep(any());
    }

    @Test
    @DisplayName("Zombie AuctionEndSaga를 감지하고 보상 처리한다")
    void recoverAuctionEndSagas_WithZombieSaga_ShouldCompensate() {
        // given
        UUID sagaId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        // 15분 전에 업데이트된 Saga (zombie timeout: 10분)
        LocalDateTime oldTime = now.minusMinutes(15);
        AuctionEndSagaContext saga = createAuctionEndSagaWithTime(sagaId, auctionId, SagaStatus.IN_PROGRESS, SagaStep.CREATE_ORDER, oldTime);

        when(auctionEndSagaRepository.findByStatus(SagaStatus.IN_PROGRESS)).thenReturn(List.of(saga));

        // when
        sagaRecoveryService.recoverAuctionEndSagas();

        // then
        verify(saga).fail(contains("Zombie 트랜잭션 감지"));
        verify(auctionEndSagaRepository, atLeastOnce()).save(saga);
        verify(compensationExecutor).executeCompensations(eq(sagaId), eq("ZombieTransactionDetected"));
        verify(auctionEndOrchestrator, never()).executeCreateOrderStep(any());
    }

    @Test
    @DisplayName("복구할 AuctionEndSaga가 없으면 로그만 남긴다")
    void recoverAuctionEndSagas_WithNoSagas_ShouldOnlyLog() {
        // given
        when(auctionEndSagaRepository.findByStatus(any())).thenReturn(List.of());

        // when
        sagaRecoveryService.recoverAuctionEndSagas();

        // then
        verifyNoInteractions(auctionEndOrchestrator);
        verifyNoInteractions(compensationExecutor);
    }

    @Test
    @DisplayName("PENDING 상태의 PaymentTimeoutSaga를 재개한다")
    void recoverPaymentTimeoutSagas_WithPendingSaga_ShouldResume() {
        // given
        UUID sagaId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        PaymentTimeoutSagaContext originalSaga = createPaymentTimeoutSaga(sagaId, auctionId, orderId, SagaStatus.PENDING);
        PaymentTimeoutSagaContext saga = spy(originalSaga);

        when(paymentTimeoutSagaRepository.findByStatus(SagaStatus.PENDING)).thenReturn(List.of(saga));

        // when
        sagaRecoveryService.recoverPaymentTimeoutSagas();

        // then
        verify(saga).start();
        verify(paymentTimeoutSagaRepository, atLeastOnce()).save(saga);
        verify(paymentTimeoutOrchestrator).executeReopenAuctionStep(saga);
    }

    @Test
    @DisplayName("Zombie PaymentTimeoutSaga를 감지하고 보상 처리한다")
    void recoverPaymentTimeoutSagas_WithZombieSaga_ShouldCompensate() {
        // given
        UUID sagaId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        LocalDateTime oldTime = now.minusMinutes(20);
        PaymentTimeoutSagaContext saga = createPaymentTimeoutSagaWithTime(sagaId, auctionId, orderId, oldTime);

        when(paymentTimeoutSagaRepository.findByStatus(SagaStatus.IN_PROGRESS)).thenReturn(List.of(saga));

        // when
        sagaRecoveryService.recoverPaymentTimeoutSagas();

        // then
        verify(saga).fail(contains("Zombie 트랜잭션 감지"));
        verify(paymentTimeoutSagaRepository, atLeastOnce()).save(saga);
        verify(compensationExecutor).executeCompensations(eq(sagaId), eq("ZombieTransactionDetected"));
    }

    @Test
    @DisplayName("OutBox 이벤트를 재발행한다")
    void recoverOutboxEvents_WithReadyToPublish_ShouldRepublish() {
        // given
        UUID outboxId = UUID.randomUUID();
        AuctionOutbox outbox = createOutbox(outboxId, OutboxStatus.FAILED, 1);

        when(outboxRepository.findReadyToPublish(100)).thenReturn(List.of(outbox));

        // when
        sagaRecoveryService.recoverOutboxEvents();

        // then
        verify(outboxPublisher).publish(outbox);
    }

    @Test
    @DisplayName("최대 재시도를 초과한 OutBox 이벤트는 건너뛴다")
    void recoverOutboxEvents_WithMaxRetriesExceeded_ShouldSkip() {
        // given
        when(outboxRepository.findReadyToPublish(100)).thenReturn(List.of());

        // when
        sagaRecoveryService.recoverOutboxEvents();

        // then
        verify(outboxPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("발행 불가 상태의 OutBox 이벤트는 건너뛴다")
    void recoverOutboxEvents_WithCannotPublish_ShouldSkip() {
        // given
        when(outboxRepository.findReadyToPublish(100)).thenReturn(List.of());

        // when
        sagaRecoveryService.recoverOutboxEvents();

        // then
        verify(outboxPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("복구할 OutBox 이벤트가 없으면 로그만 남긴다")
    void recoverOutboxEvents_WithNoEvents_ShouldOnlyLog() {
        // given
        when(outboxRepository.findReadyToPublish(100)).thenReturn(List.of());

        // when
        sagaRecoveryService.recoverOutboxEvents();

        // then
        verifyNoInteractions(outboxPublisher);
    }

    @Test
    @DisplayName("수동 복구 트리거가 전체 복구 프로세스를 실행한다")
    void triggerManualRecovery_ShouldExecuteFullRecovery() {
        // given
        when(auctionEndSagaRepository.findByStatus(any())).thenReturn(List.of());
        when(paymentTimeoutSagaRepository.findByStatus(any())).thenReturn(List.of());
        when(outboxRepository.findReadyToPublish(anyInt())).thenReturn(List.of());

        // when
        sagaRecoveryService.triggerManualRecovery();

        // then
        verify(auctionEndSagaRepository, atLeast(2)).findByStatus(any());
        verify(paymentTimeoutSagaRepository, atLeast(2)).findByStatus(any());
        verify(outboxRepository).findReadyToPublish(anyInt());
    }

    // ===== Helper Methods =====

    private AuctionEndSagaContext createAuctionEndSaga(UUID sagaId, UUID auctionId, SagaStatus status, SagaStep step) {
        return AuctionEndSagaContext.builder()
                .id(sagaId)
                .auctionId(auctionId)
                .winnerId(UUID.randomUUID())
                .winningBidId(UUID.randomUUID())
                .productSizeId(UUID.randomUUID())
                .finalPrice(100000L)
                .status(status)
                .currentStep(step)
                .retryCount(0)
                .correlationId(UUID.randomUUID())
                .build();
    }

    private AuctionEndSagaContext createAuctionEndSagaWithTime(UUID sagaId, UUID auctionId, SagaStatus status, SagaStep step, LocalDateTime updatedAt) {
        AuctionEndSagaContext saga = createAuctionEndSaga(sagaId, auctionId, status, step);
        // BaseEntity의 updatedAt을 모킹하기 위해 spy 사용
        AuctionEndSagaContext spySaga = spy(saga);
        when(spySaga.getUpdatedAt()).thenReturn(updatedAt);
        when(spySaga.getCreatedAt()).thenReturn(updatedAt);
        return spySaga;
    }

    private PaymentTimeoutSagaContext createPaymentTimeoutSaga(UUID sagaId, UUID auctionId, UUID orderId, SagaStatus status) {
        return PaymentTimeoutSagaContext.builder()
                .id(sagaId)
                .auctionId(auctionId)
                .orderId(orderId)
                .winnerId(UUID.randomUUID())
                .winningBidId(UUID.randomUUID())
                .productSizeId(UUID.randomUUID())
                .status(status)
                .currentStep(PaymentTimeoutSagaStep.REOPEN_AUCTION)
                .retryCount(0)
                .correlationId(UUID.randomUUID())
                .build();
    }

    private PaymentTimeoutSagaContext createPaymentTimeoutSagaWithTime(UUID sagaId, UUID auctionId, UUID orderId, LocalDateTime updatedAt) {
        PaymentTimeoutSagaContext saga = createPaymentTimeoutSaga(sagaId, auctionId, orderId, SagaStatus.IN_PROGRESS);
        PaymentTimeoutSagaContext spySaga = spy(saga);
        when(spySaga.getUpdatedAt()).thenReturn(updatedAt);
        when(spySaga.getCreatedAt()).thenReturn(updatedAt);
        return spySaga;
    }

    private AuctionOutbox createOutbox(UUID outboxId, OutboxStatus status, int retryCount) {
        AuctionOutbox outbox = spy(AuctionOutbox.pending(
                "AUCTION",
                UUID.randomUUID(),
                "TEST_EVENT",
                "{\"test\":\"data\"}",
                UUID.randomUUID()
        ));

        when(outbox.getId()).thenReturn(outboxId);
        when(outbox.getStatus()).thenReturn(status);
        when(outbox.getRetryCount()).thenReturn(retryCount);

        return outbox;
    }
}
