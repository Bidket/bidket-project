package com.bidket.order.application.refund.facade;

import com.bidket.order.application.refund.info.RefundSummaryInfo;
import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.model.OrderStatus;
import com.bidket.order.domain.order.repository.OrderRepository;
import com.bidket.order.domain.payment.model.Payment;
import com.bidket.order.domain.payment.model.PaymentStatus;
import com.bidket.order.domain.payment.repository.PaymentRepository;
import com.bidket.order.domain.refund.model.Refund;
import com.bidket.order.domain.refund.model.RefundStatus;
import com.bidket.order.domain.refund.repository.RefundRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TODO: Payple 연동

@Service
@RequiredArgsConstructor
public class RefundFacade {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Refund createRefund(
            UUID userId,
            UUID paymentId,
            Long refundAmount,
            Long refundPointAmount,
            String reason
    ) {
        if (refundRepository.existsByPaymentIdAndStatusIn(
                paymentId,
                List.of(RefundStatus.REQUESTED, RefundStatus.APPROVED)
        )) {
            throw new IllegalStateException("이미 환불이 진행 중이거나 완료된 결제입니다.");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));

        if (!payment.userId().equals(userId)) {
            throw new IllegalStateException("본인 결제만 환불할 수 있습니다.");
        }
        if (payment.status() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("환불 가능한 결제 상태가 아닙니다.");
        }
        if (!payment.amount().equals(refundAmount)) {
            throw new IllegalStateException("환불 금액이 결제 금액과 다릅니다.");
        }

        Order order = orderRepository.findById(payment.orderId())
                .orElseThrow(() -> new IllegalStateException("주문 정보를 찾을 수 없습니다."));

        if (order.status() != OrderStatus.PAID) {
            throw new IllegalStateException("환불 가능한 주문 상태가 아닙니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        Refund refund = Refund.request(
                userId,
                paymentId,
                refundAmount,
                refundPointAmount,
                reason,
                now
        );

        paymentRepository.save(payment.requestRefund(now));
        orderRepository.save(order.requestRefund(now));

        return refundRepository.save(refund);
    }

    @Transactional
    public Refund approveRefund(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalStateException("환불 정보를 찾을 수 없습니다."));

        if (refund.status() != RefundStatus.REQUESTED) {
            throw new IllegalStateException("승인 가능한 환불 상태가 아닙니다.");
        }

        Payment payment = paymentRepository.findById(refund.paymentId())
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));

        if (payment.status() != PaymentStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("환불 승인 가능한 결제 상태가 아닙니다.");
        }

        Order order = orderRepository.findById(payment.orderId())
                .orElseThrow(() -> new IllegalStateException("주문 정보를 찾을 수 없습니다."));

        if (order.status() != OrderStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("환불 승인 가능한 주문 상태가 아닙니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        Refund approvedRefund = refund.approve(now);
        Payment refundedPayment = payment.completeRefund(now);
        Order refundedOrder = order.completeRefund(now);

        paymentRepository.save(refundedPayment);
        orderRepository.save(refundedOrder);
        return refundRepository.save(approvedRefund);
    }

    @Transactional(readOnly = true)
    public Page<RefundSummaryInfo> getMyRefunds(UUID userId, Pageable pageable) {
        return refundRepository.findByUserId(userId, pageable)
                .map(RefundSummaryInfo::from);
    }
}