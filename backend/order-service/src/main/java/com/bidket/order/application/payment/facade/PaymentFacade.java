package com.bidket.order.application.payment.facade;

import com.bidket.order.domain.payment.model.Payment;
import com.bidket.order.domain.payment.model.PaymentMethod;
import com.bidket.order.domain.payment.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentRepository paymentRepository;

    public Payment createPayment(UUID orderId, String method, Long amount, Long usedPointAmount) {
        Payment payment = Payment.create(
                orderId,
                PaymentMethod.valueOf(method.toUpperCase()),
                amount,
                usedPointAmount,
                LocalDateTime.now()
        );
        // TODO: Payple 연동 후 상태 업데이트 처리 추가 예정
        return paymentRepository.save(payment);
    }
}