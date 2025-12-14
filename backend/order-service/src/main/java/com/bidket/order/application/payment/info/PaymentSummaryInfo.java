package com.bidket.order.application.payment.info;

import com.bidket.order.domain.payment.model.Payment;
import com.bidket.order.domain.payment.model.PaymentMethod;
import com.bidket.order.domain.payment.model.PaymentStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
public class PaymentSummaryInfo {

    private final UUID paymentId;
    private final UUID orderId;
    private final Long amount;
    private final Long usedPointAmount;
    private final PaymentStatus status;
    private final PaymentMethod method;
    private final LocalDateTime updatedAt;

    public PaymentSummaryInfo(
            UUID paymentId,
            UUID orderId,
            Long amount,
            Long usedPointAmount,
            PaymentStatus status,
            PaymentMethod method,
            LocalDateTime updatedAt
    ) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.usedPointAmount = usedPointAmount;
        this.status = status;
        this.method = method;
        this.updatedAt = updatedAt;
    }

    public static PaymentSummaryInfo from(Payment payment) {
        return new PaymentSummaryInfo(
                payment.id(),
                payment.orderId(),
                payment.amount(),
                payment.usedPointAmount(),
                payment.status(),
                payment.method(),
                payment.updatedAt()
        );
    }
}