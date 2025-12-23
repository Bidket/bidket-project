package com.bidket.order.application.refund.info;

import com.bidket.order.domain.refund.model.Refund;
import com.bidket.order.domain.refund.model.RefundStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record RefundSummaryInfo(
        UUID refundId,
        UUID paymentId,
        Long refundAmount,
        Long refundedPointAmount,
        RefundStatus status,
        LocalDateTime approvedAt
) {

    public static RefundSummaryInfo from(Refund refund) {
        return new RefundSummaryInfo(
                refund.id(),
                refund.paymentId(),
                refund.refundAmount(),
                refund.refundedPointAmount(),
                refund.status(),
                refund.approvedAt()
        );
    }
}