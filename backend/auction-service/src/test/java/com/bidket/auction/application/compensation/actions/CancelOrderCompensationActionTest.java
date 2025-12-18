package com.bidket.auction.application.compensation.actions;

import com.bidket.auction.infrastructure.order.OrderEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * CancelOrderCompensationAction 단위 테스트
 * 주문 취소 보상 로직 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CancelOrderCompensationAction 테스트")
class CancelOrderCompensationActionTest {

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private CancelOrderCompensationAction compensationAction;

    private UUID sagaId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        sagaId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("주문 취소 요청 이벤트 발행 성공")
    void execute_Success() throws Exception {
        // given
        when(orderEventProducer.publishCancelOrderRequest(
                any(UUID.class),
                isNull(),
                eq("SAGA_COMPENSATION"),
                any(UUID.class)
        )).thenReturn(null); // AuctionOutbox를 반환하지만 사용하지 않음

        // when
        compensationAction.execute(sagaId, orderId, "{}");

        // then
        verify(orderEventProducer).publishCancelOrderRequest(
                eq(orderId),
                isNull(),
                eq("SAGA_COMPENSATION"),
                eq(sagaId)
        );
    }

    @Test
    @DisplayName("orderId가 null이면 취소 요청 Skip")
    void execute_NullOrderId_Skip() throws Exception {
        // given
        UUID nullOrderId = null;

        // when
        compensationAction.execute(sagaId, nullOrderId, "{}");

        // then
        verify(orderEventProducer, never()).publishCancelOrderRequest(
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("OrderEventProducer 예외 발생 시 예외 전파")
    void execute_ProducerThrowsException_PropagatesException() {
        // given
        RuntimeException exception = new RuntimeException("Kafka connection failed");
        doThrow(exception).when(orderEventProducer).publishCancelOrderRequest(
                any(UUID.class),
                any(),
                eq("SAGA_COMPENSATION"),
                any(UUID.class)
        );

        // when & then
        assertThatThrownBy(() -> compensationAction.execute(sagaId, orderId, "{}"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kafka connection failed");

        verify(orderEventProducer).publishCancelOrderRequest(
                eq(orderId),
                isNull(),
                eq("SAGA_COMPENSATION"),
                eq(sagaId)
        );
    }

    @Test
    @DisplayName("취소 사유는 SAGA_COMPENSATION으로 설정됨")
    void execute_SetsCancellationReason() throws Exception {
        // given
        when(orderEventProducer.publishCancelOrderRequest(
                any(UUID.class),
                any(),
                any(String.class),
                any(UUID.class)
        )).thenReturn(null);

        // when
        compensationAction.execute(sagaId, orderId, "{}");

        // then
        verify(orderEventProducer).publishCancelOrderRequest(
                eq(orderId),
                any(),
                eq("SAGA_COMPENSATION"),
                eq(sagaId)
        );
    }

    @Test
    @DisplayName("auctionId는 null로 전달됨 (Order Service가 조회)")
    void execute_AuctionIdIsNull() throws Exception {
        // given
        when(orderEventProducer.publishCancelOrderRequest(
                any(UUID.class),
                isNull(),
                any(String.class),
                any(UUID.class)
        )).thenReturn(null);

        // when
        compensationAction.execute(sagaId, orderId, "{}");

        // then
        verify(orderEventProducer).publishCancelOrderRequest(
                eq(orderId),
                isNull(), // auctionId는 null로 전달
                eq("SAGA_COMPENSATION"),
                eq(sagaId)
        );
    }
}
