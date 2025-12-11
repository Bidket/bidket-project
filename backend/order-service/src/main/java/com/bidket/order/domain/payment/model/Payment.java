package com.bidket.order.domain.payment.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Payment(
        UUID id,
        UUID userId,
        UUID orderId,
        PaymentMethod method,
        Long amount,
        Long usedPointAmount,
        PaymentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public Payment {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (usedPointAmount != null && usedPointAmount < 0) {
            throw new IllegalArgumentException("usedPointAmount must be >= 0");
        }
    }

    public static Payment create(
            UUID userId,
            UUID orderId,
            PaymentMethod method,
            Long amount,
            Long usedPointAmount,
            LocalDateTime now
    ) {
        return new Payment(
                UUID.randomUUID(),
                userId,
                orderId,
                method,
                amount,
                usedPointAmount,
                PaymentStatus.PENDING,
                now,
                now
        );
    }

    public Payment approve(LocalDateTime approvedAt) {
        return new Payment(
                this.id,
                this.userId,
                this.orderId,
                this.method,
                this.amount,
                this.usedPointAmount,
                PaymentStatus.SUCCESS,
                this.createdAt,
                approvedAt
        );
    }

    public Payment fail(LocalDateTime failedAt) {
        return new Payment(
                this.id,
                this.userId,
                this.orderId,
                this.method,
                this.amount,
                this.usedPointAmount,
                PaymentStatus.FAILED,
                this.createdAt,
                failedAt
        );
    }

    public LocalDateTime approvedAt() {
        return this.status == PaymentStatus.SUCCESS ? this.updatedAt : null;
    }
}