package com.bidket.order.domain.payment.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Payment(
        UUID id,
        UUID orderId,
        PaymentMethod method,
        Long amount,
        Long usedPointAmount,
        PaymentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    // TODO: Payple 연동 후 상태 업데이트 처리 추가 예정
    public Payment {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (usedPointAmount != null && usedPointAmount < 0) {
            throw new IllegalArgumentException("usedPointAmount must be >= 0");
        }
    }

    public static Payment create(
            UUID orderId,
            PaymentMethod method,
            Long amount,
            Long usedPointAmount,
            LocalDateTime now
    ) {
        return new Payment(
                null,
                orderId,
                method,
                amount,
                usedPointAmount,
                PaymentStatus.PENDING,
                now,
                now
        );
    }
}