package com.bidket.order.application.payment.facade;

import com.bidket.order.application.payment.info.PaymentSummaryInfo;
import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.model.OrderStatus;
import com.bidket.order.domain.order.repository.OrderRepository;
import com.bidket.order.domain.payment.model.Payment;
import com.bidket.order.domain.payment.model.PaymentMethod;
import com.bidket.order.domain.payment.model.PaymentStatus;
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("주문을 찾을 수 없습니다."));

        validatePayableOrder(order, userId);
        validateAmount(order, amount);

        if (paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.PENDING)) {
            throw new IllegalStateException("이미 진행 중인 결제가 존재합니다.");
        }

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
     * 결제 성공 시 후속 처리 1. 주문 상태를 PAID로 변경 2. PAYMENT_COMPLETED 이벤트 발행
     */
    private void handlePaymentSuccess(Payment payment) {
        try {
            // 1. 주문 조회
            Order order = orderRepository.findById(payment.orderId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "주문을 찾을 수 없습니다: " + payment.orderId()));

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

    @Transactional
    public Payment confirmPayment(
            UUID userId,
            String paymentKey,
            UUID orderId,
            Long amount
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("주문을 찾을 수 없습니다."));

        validatePaymentKey(paymentKey);
        validatePayableOrder(order, userId);
        validateAmount(order, amount);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));

        if (payment.status() != PaymentStatus.PENDING) {
            throw new IllegalStateException("승인 가능한 결제 상태가 아닙니다.");
        }

        Payment approved = payment.approve(LocalDateTime.now());
        Payment saved = paymentRepository.save(approved);

        Order paidOrder = order.markPaid(LocalDateTime.now());
        orderRepository.save(paidOrder);

        return saved;
    }

    @Transactional
    public Payment failPayment(
            UUID userId,
            UUID orderId,
            String errorCode,
            String errorMessage
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("주문을 찾을 수 없습니다."));

        validatePayableOrder(order, userId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));

        if (payment.status() != PaymentStatus.PENDING) {
            throw new IllegalStateException("실패 처리 가능한 결제 상태가 아닙니다.");
        }

        Payment failed = payment.fail(LocalDateTime.now());
        Payment saved = paymentRepository.save(failed);

        Order canceled = order.cancelByPaymentFail(LocalDateTime.now(), errorCode, errorMessage);
        orderRepository.save(canceled);

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<PaymentSummaryInfo> getMyPayments(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable)
                .map(PaymentSummaryInfo::from);
    }

    @Transactional(readOnly = true)
    public Page<PaymentSummaryInfo> getMyPayments(UUID userId, PaymentStatus status,
            Pageable pageable) {
        return (status == null)
                ? paymentRepository.findByUserId(userId, pageable).map(PaymentSummaryInfo::from)
                : paymentRepository.findByUserIdAndStatus(userId, status, pageable)
                        .map(PaymentSummaryInfo::from);
    }

    private void validatePayableOrder(Order order, UUID userId) {
        if (!order.userId().equals(userId)) {
            throw new IllegalStateException("본인 주문만 결제할 수 있습니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (order.isPaymentExpired(now)) {
            orderRepository.save(order.markExpired(now));
            throw new IllegalStateException("결제 시간이 만료된 주문입니다.");
        }
        if (order.status() != OrderStatus.PAYMENT) {
            throw new IllegalStateException("결제 가능한 주문 상태가 아닙니다.");
        }
    }

    private void validateAmount(Order order, Long amount) {
        if (!order.amount().equals(amount)) {
            throw new IllegalStateException("결제 금액이 주문 금액과 다릅니다.");
        }
    }

    private void validatePaymentKey(String paymentKey) {
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new IllegalStateException("paymentKey가 비어있습니다.");
        }
    }
}