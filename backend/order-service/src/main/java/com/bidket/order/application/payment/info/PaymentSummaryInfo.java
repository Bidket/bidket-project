package com.bidket.order.application.payment.info;

import com.bidket.order.domain.payment.model.Payment;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentSummaryInfo {

    private final UUID paymentId;
    private final UUID orderId;
    private final Long amount;
    private final Long usedPointAmount;
    private final String status;
    private final String method;
    private final LocalDateTime updatedAt;

    public static PaymentSummaryInfo from(Payment payment) {
        return PaymentSummaryInfo.builder()
                .paymentId(payment.id())
                .orderId(payment.orderId())
                .amount(payment.amount())
                .usedPointAmount(payment.usedPointAmount())
                .status(payment.status().name())
                .method(payment.method().name())
                .updatedAt(payment.updatedAt())
                .build();
    }
}