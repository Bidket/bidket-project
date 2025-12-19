package com.bidket.order.domain.refund.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Refund(
        UUID id,
        UUID userId,
        UUID paymentId,
        Long refundAmount,
        Long refundedPointAmount,
        RefundStatus status,
        String reason,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt
) {

    public Refund {
        if (refundAmount == null || refundAmount <= 0) {
            throw new IllegalArgumentException("refundAmount must be positive");
        }
        if (refundedPointAmount != null && refundedPointAmount < 0) {
            throw new IllegalArgumentException("refundedPointAmount must be >= 0");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }

    public static Refund request(
            UUID userId,
            UUID paymentId,
            Long refundAmount,
            Long refundPointAmount,
            String reason,
            LocalDateTime now
    ) {
        return new Refund(
                null,
                userId,
                paymentId,
                refundAmount,
                refundPointAmount,
                RefundStatus.REQUESTED,
                reason,
                now,
                null
        );
    }
}