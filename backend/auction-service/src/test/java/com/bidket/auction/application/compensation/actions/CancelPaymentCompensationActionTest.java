package com.bidket.auction.application.compensation.actions;

import com.bidket.auction.infrastructure.event.payment.PaymentEventProducer;
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
 * CancelPaymentCompensationAction 단위 테스트
 * 결제 취소 보상 로직 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CancelPaymentCompensationAction 테스트")
class CancelPaymentCompensationActionTest {

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private CancelPaymentCompensationAction compensationAction;

    private UUID sagaId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        sagaId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("결제 취소 요청 이벤트 발행 성공")
    void execute_Success() throws Exception {
        // given
        doNothing().when(paymentEventProducer).publishCancelPaymentRequest(
                any(UUID.class),
                isNull(),
                isNull(),
                eq("SAGA_COMPENSATION"),
                any(UUID.class)
        );

        // when
        compensationAction.execute(sagaId, paymentId, "{}");

        // then
        verify(paymentEventProducer).publishCancelPaymentRequest(
                eq(paymentId),
                isNull(),
                isNull(),
                eq("SAGA_COMPENSATION"),
                eq(sagaId)
        );
    }

    @Test
    @DisplayName("paymentId가 null이면 취소 요청 Skip")
    void execute_NullPaymentId_Skip() throws Exception {
        // given
        UUID nullPaymentId = null;

        // when
        compensationAction.execute(sagaId, nullPaymentId, "{}");

        // then
        verify(paymentEventProducer, never()).publishCancelPaymentRequest(
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("PaymentEventProducer 예외 발생 시 예외 전파")
    void execute_ProducerThrowsException_PropagatesException() {
        // given
        RuntimeException exception = new RuntimeException("Payment service unavailable");
        doThrow(exception).when(paymentEventProducer).publishCancelPaymentRequest(
                any(UUID.class),
                any(),
                any(),
                eq("SAGA_COMPENSATION"),
                any(UUID.class)
        );

        // when & then
        assertThatThrownBy(() -> compensationAction.execute(sagaId, paymentId, "{}"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment service unavailable");

        verify(paymentEventProducer).publishCancelPaymentRequest(
                eq(paymentId),
                isNull(),
                isNull(),
                eq("SAGA_COMPENSATION"),
                eq(sagaId)
        );
    }

    @Test
    @DisplayName("취소 사유는 SAGA_COMPENSATION으로 설정됨")
    void execute_SetsCancellationReason() throws Exception {
        // given
        doNothing().when(paymentEventProducer).publishCancelPaymentRequest(
                any(UUID.class),
                any(),
                any(),
                any(String.class),
                any(UUID.class)
        );

        // when
        compensationAction.execute(sagaId, paymentId, "{}");

        // then
        verify(paymentEventProducer).publishCancelPaymentRequest(
                eq(paymentId),
                any(),
                any(),
                eq("SAGA_COMPENSATION"),
                eq(sagaId)
        );
    }

    @Test
    @DisplayName("orderId와 auctionId는 null로 전달됨 (Payment Service가 조회)")
    void execute_OrderIdAndAuctionIdAreNull() throws Exception {
        // given
        doNothing().when(paymentEventProducer).publishCancelPaymentRequest(
                any(UUID.class),
                isNull(),
                isNull(),
                any(String.class),
                any(UUID.class)
        );

        // when
        compensationAction.execute(sagaId, paymentId, "{}");

        // then
        verify(paymentEventProducer).publishCancelPaymentRequest(
                eq(paymentId),
                isNull(), // orderId는 null로 전달
                isNull(), // auctionId는 null로 전달
                eq("SAGA_COMPENSATION"),
                eq(sagaId)
        );
    }
}
