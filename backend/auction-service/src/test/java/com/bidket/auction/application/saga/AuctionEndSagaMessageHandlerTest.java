package com.bidket.auction.application.saga;

import com.bidket.auction.domain.saga.model.AuctionEndSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;
import com.bidket.auction.domain.saga.model.SagaStep;
import com.bidket.auction.domain.saga.repository.AuctionEndSagaContextRepository;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuctionEndSagaMessageHandler 단위 테스트
 * Order Service 이벤트 처리 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionEndSagaMessageHandler 테스트")
class AuctionEndSagaMessageHandlerTest {

    @Mock
    private AuctionEndSagaOrchestrator orchestrator;

    @Mock
    private AuctionEndSagaContextRepository sagaRepository;

    @InjectMocks
    private AuctionEndSagaMessageHandler messageHandler;

    private UUID sagaId;
    private UUID orderId;
    private UUID auctionId;
    private UUID winnerId;
    private UUID winningBidId;
    private UUID productSizeId;
    private AuctionEndSagaContext sagaContext;

    @BeforeEach
    void setUp() {
        sagaId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        auctionId = UUID.randomUUID();
        winnerId = UUID.randomUUID();
        winningBidId = UUID.randomUUID();
        productSizeId = UUID.randomUUID();

        sagaContext = AuctionEndSagaContext.builder()
                .id(sagaId)
                .auctionId(auctionId)
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .productSizeId(productSizeId)
                .finalPrice(150000L)
                .status(SagaStatus.IN_PROGRESS)
                .currentStep(SagaStep.CREATE_ORDER)
                .correlationId(UUID.randomUUID())
                .build();
    }

    @Test
    @DisplayName("ORDER_CREATED 이벤트 처리 성공")
    void handleOrderCreated_Success() {
        // given
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("sagaId", sagaId.toString());
        data.put("orderId", orderId.toString());
        data.put("auctionId", auctionId.toString());
        payload.put("data", data);

        when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(sagaContext));
        when(sagaRepository.save(any(AuctionEndSagaContext.class))).thenReturn(sagaContext);

        // when
        messageHandler.handleOrderCreated(payload);

        // then
        verify(sagaRepository).findById(sagaId);
        verify(sagaRepository).save(any(AuctionEndSagaContext.class));
        verify(orchestrator).executeMarkWinningBidStep(any(AuctionEndSagaContext.class));
    }

    @Test
    @DisplayName("ORDER_CREATED 이벤트 처리 시 Saga를 찾을 수 없으면 예외 발생")
    void handleOrderCreated_SagaNotFound() {
        // given
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("sagaId", sagaId.toString());
        data.put("orderId", orderId.toString());
        data.put("auctionId", auctionId.toString());
        payload.put("data", data);

        when(sagaRepository.findById(sagaId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> messageHandler.handleOrderCreated(payload))
                .isInstanceOf(AuctionDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuctionErrorCode.SAGA_NOT_FOUND);

        verify(sagaRepository).findById(sagaId);
        verify(sagaRepository, never()).save(any());
        verify(orchestrator, never()).executeMarkWinningBidStep(any());
    }

    @Test
    @DisplayName("ORDER_CREATED 이벤트 처리 시 data 필드가 없으면 예외 발생")
    void handleOrderCreated_InvalidEventStructure() {
        // given: data 필드가 없는 잘못된 구조
        Map<String, Object> payload = new HashMap<>();
        payload.put("sagaId", sagaId.toString());
        payload.put("orderId", orderId.toString());

        // when & then
        assertThatThrownBy(() -> messageHandler.handleOrderCreated(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid event structure");

        verify(sagaRepository, never()).findById(any());
        verify(orchestrator, never()).executeMarkWinningBidStep(any());
    }

    @Test
    @DisplayName("ORDER_CREATION_FAILED 이벤트 처리 성공")
    void handleOrderCreationFailed_Success() {
        // given
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("sagaId", sagaId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("failureReason", "Insufficient inventory");
        payload.put("data", data);

        // when
        messageHandler.handleOrderCreationFailed(payload);

        // then
        verify(orchestrator).compensate(eq(sagaId), eq("Insufficient inventory"));
    }

    @Test
    @DisplayName("ORDER_CREATION_FAILED 이벤트 처리 시 data 필드가 없으면 예외 발생")
    void handleOrderCreationFailed_InvalidEventStructure() {
        // given: data 필드가 없는 잘못된 구조
        Map<String, Object> payload = new HashMap<>();
        payload.put("sagaId", sagaId.toString());
        payload.put("failureReason", "Error");

        // when & then
        assertThatThrownBy(() -> messageHandler.handleOrderCreationFailed(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid event structure");

        verify(orchestrator, never()).compensate(any(), any());
    }

    @Test
    @DisplayName("UUID 파싱 - String 형식")
    void parseUuid_FromString() {
        // given
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("sagaId", sagaId.toString()); // String 형식
        data.put("orderId", orderId.toString());
        data.put("auctionId", auctionId.toString());
        payload.put("data", data);

        when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(sagaContext));
        when(sagaRepository.save(any(AuctionEndSagaContext.class))).thenReturn(sagaContext);

        // when
        messageHandler.handleOrderCreated(payload);

        // then
        verify(sagaRepository).findById(sagaId);
        verify(orchestrator).executeMarkWinningBidStep(any());
    }

    @Test
    @DisplayName("UUID 파싱 - UUID 객체 형식")
    void parseUuid_FromUuidObject() {
        // given
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("sagaId", sagaId); // UUID 객체 형식
        data.put("orderId", orderId);
        data.put("auctionId", auctionId);
        payload.put("data", data);

        when(sagaRepository.findById(sagaId)).thenReturn(Optional.of(sagaContext));
        when(sagaRepository.save(any(AuctionEndSagaContext.class))).thenReturn(sagaContext);

        // when
        messageHandler.handleOrderCreated(payload);

        // then
        verify(sagaRepository).findById(sagaId);
        verify(orchestrator).executeMarkWinningBidStep(any());
    }
}
