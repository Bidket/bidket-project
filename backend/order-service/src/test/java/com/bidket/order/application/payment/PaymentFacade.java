package com.bidket.order.application.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bidket.order.application.payment.facade.PaymentFacade;
import com.bidket.order.application.payment.info.PaymentSummaryInfo;
import com.bidket.order.domain.payment.model.Payment;
import com.bidket.order.domain.payment.model.PaymentMethod;
import com.bidket.order.domain.payment.model.PaymentStatus;
import com.bidket.order.domain.payment.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID orderId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-1234567890ab");
        String method = "CARD";
        Long amount = 50_000L;
        Long usedPointAmount = 10_000L;

        // save()가 받은 Payment를 그대로 돌려주도록 설정
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Payment.class));

        // when
        // ➜ PaymentFacade 시그니처에 맞춰서 5개 인자 전달
        Payment result = paymentFacade.createPayment(
                userId,
                orderId,
                method,
                amount,
                usedPointAmount
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull(); // 내부에서 생성된 ID
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
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID orderId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-1234567890ab");
        String method = "CARD";
        Long amount = 50_000L;
        Long usedPointAmount = 10_000L;

        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new RuntimeException("DB error"));

        // when & then
        assertThrows(RuntimeException.class,
                () -> paymentFacade.createPayment(
                        userId,
                        orderId,
                        method,
                        amount,
                        usedPointAmount
                ));
    }

    @Test
    @DisplayName("결제 목록 조회 성공 - 회원 ID와 페이징 정보로 결제 내역을 조회한다")
    void getPaymentList_success() {
        // given
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID orderId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-1234567890ab");
        UUID paymentId = UUID.fromString("cd00b844-3133-4541-afb2-25c490958008");
        Long amount = 50_000L;
        Long usedPointAmount = 10_000L;
        LocalDateTime now = LocalDateTime.of(2025, 12, 8, 22, 47, 46);

        Pageable pageable = PageRequest.of(0, 20);

        Payment payment = new Payment(
                paymentId,
                userId,
                orderId,
                PaymentMethod.CARD,
                amount,
                usedPointAmount,
                PaymentStatus.PENDING,
                now,
                now
        );

        Page<Payment> paymentPage = new PageImpl<>(
                List.of(payment),
                pageable,
                1
        );

        when(paymentRepository.findByUserId(userId, pageable))
                .thenReturn(paymentPage);

        // when
        Page<PaymentSummaryInfo> result = paymentFacade.getMyPayments(userId, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);

        PaymentSummaryInfo info = result.getContent().get(0);
        assertThat(info.getPaymentId()).isEqualTo(paymentId);
        assertThat(info.getOrderId()).isEqualTo(orderId);
        assertThat(info.getAmount()).isEqualTo(amount);
        assertThat(info.getUsedPointAmount()).isEqualTo(usedPointAmount);
        assertThat(info.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(info.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(info.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("결제 목록 조회 실패 - Repository에서 예외 발생 시 예외를 전파한다")
    void getPaymentList_fail_whenRepositoryThrows() {
        // given
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Pageable pageable = PageRequest.of(0, 20);

        when(paymentRepository.findByUserId(userId, pageable))
                .thenThrow(new RuntimeException("DB error"));

        // when & then
        assertThrows(RuntimeException.class,
                () -> paymentFacade.getMyPayments(userId, pageable));
    }
}