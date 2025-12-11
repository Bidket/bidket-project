package com.bidket.order.presentation.payment.dto.response;

import com.bidket.order.application.payment.info.PaymentSummaryInfo;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentSummaryResponse(
        UUID paymentId,
        UUID orderId,
        Long amount,
        Long usedPointAmount,
        String status,
        String method,
        LocalDateTime updatedAt
) {

    public static PaymentSummaryResponse from(PaymentSummaryInfo info) {
        return new PaymentSummaryResponse(
                info.getPaymentId(),
                info.getOrderId(),
                info.getAmount(),
                info.getUsedPointAmount(),
                info.getStatus().name(),
                info.getMethod().name(),
                info.getUpdatedAt()
        );
    }
}