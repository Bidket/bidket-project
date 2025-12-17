package com.bidket.order.application.payment.facade;

import com.bidket.order.application.payment.info.PaymentSummaryInfo;
import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.repository.OrderRepository;
import com.bidket.order.domain.payment.model.Payment;
import com.bidket.order.domain.payment.model.PaymentMethod;
import com.bidket.order.domain.payment.repository.PaymentRepository;
import com.bidket.order.infrastructure.kafka.producer.AuctionEventProducer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AuctionEventProducer auctionEventProducer;
    private final Clock clock;

    @Transactional
    public Payment createPayment(
            UUID userId,
            UUID orderId,
            String method,
            Long amount,
            Long usedPointAmount
    ) {
        Payment payment = Payment.create(
                userId,
                orderId,
                PaymentMethod.valueOf(method.toUpperCase()),
                amount,
                usedPointAmount,
                LocalDateTime.now(clock)
        );

        // TODO: Payple PG 연동 로직 추가
        // 여기서는 PG 연동이 성공했다고 가정하고 즉시 승인 처리
        Payment approvedPayment = payment.approve(LocalDateTime.now(clock));
        Payment saved = paymentRepository.save(approvedPayment);

        // 결제 성공 시 처리
        if (saved.status() == com.bidket.order.domain.payment.model.PaymentStatus.SUCCESS) {
            handlePaymentSuccess(saved);
        }

        return saved;
    }

    /**
     * 결제 성공 시 후속 처리
     * 1. 주문 상태를 PAID로 변경
     * 2. PAYMENT_COMPLETED 이벤트 발행
     */
    private void handlePaymentSuccess(Payment payment) {
        try {
            // 1. 주문 조회
            Order order = orderRepository.findById(payment.orderId())
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + payment.orderId()));

            // 2. 주문 상태를 PAID로 변경
            LocalDateTime now = LocalDateTime.now(clock);
            Order paidOrder = order.markPaid(now);
            orderRepository.save(paidOrder);

            // 3. PAYMENT_COMPLETED 이벤트 발행 (OutBox 패턴)
            auctionEventProducer.publishPaymentCompleted(
                    order.id(),
                    payment.id(),
                    order.auctionId(),
                    payment.userId(),
                    payment.amount(),
                    UUID.randomUUID() // correlationId 생성
            );

            log.info("[PaymentFacade] 결제 완료 처리: paymentId={}, orderId={}, amount={}",
                    payment.id(), order.id(), payment.amount());

        } catch (Exception e) {
            log.error("[PaymentFacade] 결제 완료 처리 중 오류 발생: paymentId={}, orderId={}",
                    payment.id(), payment.orderId(), e);
            throw new RuntimeException("결제 완료 처리 실패", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<PaymentSummaryInfo> getMyPayments(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable)
                .map(PaymentSummaryInfo::from);
    }
}