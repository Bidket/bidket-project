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
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("결제 승인 가능한 상태가 아닙니다.");
        }
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
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("결제 실패 처리 가능한 상태가 아닙니다.");
        }
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

    public Payment requestRefund(LocalDateTime now) {
        if (this.status != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("환불 요청 가능한 결제 상태가 아닙니다.");
        }
        return new Payment(
                this.id,
                this.userId,
                this.orderId,
                this.method,
                this.amount,
                this.usedPointAmount,
                PaymentStatus.REFUND_REQUESTED,
                this.createdAt,
                now
        );
    }

    public Payment completeRefund(LocalDateTime now) {
        if (this.status != PaymentStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("환불 완료로 전이 가능한 결제 상태가 아닙니다.");
        }
        return new Payment(
                this.id,
                this.userId,
                this.orderId,
                this.method,
                this.amount,
                this.usedPointAmount,
                PaymentStatus.REFUNDED,
                this.createdAt,
                now
        );
    }

    public LocalDateTime approvedAt() {
        return this.status == PaymentStatus.SUCCESS ? this.updatedAt : null;
    }
}