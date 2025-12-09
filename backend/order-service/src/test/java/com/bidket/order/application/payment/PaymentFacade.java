package com.bidket.order.application.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.bidket.order.application.payment.facade.PaymentFacade;
import com.bidket.order.domain.payment.model.Payment;
import com.bidket.order.domain.payment.model.PaymentMethod;
import com.bidket.order.domain.payment.model.PaymentStatus;
import com.bidket.order.domain.payment.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentFacade paymentFacade;

    @Test
    @DisplayName("결제 요청 생성 성공 - 결제 정보가 저장되고 PENDING 상태로 반환된다")
    void createPayment_success() {
        // given
        UUID orderId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-1234567890ab");
        String method = "CARD";
        Long amount = 50_000L;
        Long usedPointAmount = 10_000L;

        UUID paymentId = UUID.fromString("61ef50cf-0e17-43e3-baf8-f8b65e3eea47");
        LocalDateTime now = LocalDateTime.of(2025, 12, 7, 22, 2, 39);

        Payment savedPayment = new Payment(
                paymentId,
                orderId,
                PaymentMethod.CARD,
                amount,
                usedPointAmount,
                PaymentStatus.PENDING,
                now,
                now
        );

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        // when
        Payment result = paymentFacade.createPayment(orderId, method, amount, usedPointAmount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(paymentId);
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.method()).isEqualTo(PaymentMethod.CARD);
        assertThat(result.amount()).isEqualTo(amount);
        assertThat(result.usedPointAmount()).isEqualTo(usedPointAmount);
        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("결제 요청 생성 실패 - Repository에서 예외 발생 시 예외를 전파한다")
    void createPayment_fail_whenRepositoryThrows() {
        // given
        UUID orderId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-1234567890ab");
        String method = "CARD";
        Long amount = 50_000L;
        Long usedPointAmount = 10_000L;

        doThrow(new RuntimeException("DB error"))
                .when(paymentRepository)
                .save(any(Payment.class));

        // when & then
        assertThrows(RuntimeException.class,
                () -> paymentFacade.createPayment(orderId, method, amount, usedPointAmount));
    }
}