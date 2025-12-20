package com.bidket.order.domain.order.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Order(
        UUID id,
        UUID userId,
        UUID auctionId,
        UUID shoeId,
        OrderStatus status,
        Long amount,
        Long usedPointAmount,
        LocalDateTime paymentExpiredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public Order {
        Long safeUsedPoint = usedPointAmount == null ? 0L : usedPointAmount;

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (safeUsedPoint < 0) {
            throw new IllegalArgumentException("usedPointAmount must be >= 0");
        }
        if (safeUsedPoint > amount) {
            throw new IllegalArgumentException("usedPointAmount cannot be greater than amount");
        }

        usedPointAmount = safeUsedPoint;
    }

    public static Order createForPayment(
            UUID userId,
            UUID auctionId,
            UUID shoeId,
            Long amount,
            Long usedPointAmount,
            LocalDateTime paymentExpiredAt,
            LocalDateTime now
    ) {
        return new Order(
                null,
                userId,
                auctionId,
                shoeId,
                OrderStatus.PAYMENT,
                amount,
                usedPointAmount,
                paymentExpiredAt,
                now,
                now
        );
    }

    public static Order of(
            UUID id,
            UUID userId,
            UUID auctionId,
            UUID shoeId,
            OrderStatus status,
            Long amount,
            Long usedPointAmount,
            LocalDateTime paymentExpiredAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new Order(
                id,
                userId,
                auctionId,
                shoeId,
                status,
                amount,
                usedPointAmount,
                paymentExpiredAt,
                createdAt,
                updatedAt
        );
    }

    public Order requestRefund(LocalDateTime now) {
        if (this.status != OrderStatus.PAID) {
            throw new IllegalStateException("환불 요청 가능한 주문 상태가 아닙니다.");
        }
        return new Order(
                this.id,
                this.userId,
                this.auctionId,
                this.shoeId,
                OrderStatus.REFUND_REQUESTED,
                this.amount,
                this.usedPointAmount,
                this.paymentExpiredAt,
                this.createdAt,
                now
        );
    }

    public Order completeRefund(LocalDateTime now) {
        if (this.status != OrderStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("환불 완료로 전이 가능한 주문 상태가 아닙니다.");
        }
        return new Order(
                this.id,
                this.userId,
                this.auctionId,
                this.shoeId,
                OrderStatus.REFUNDED,
                this.amount,
                this.usedPointAmount,
                this.paymentExpiredAt,
                this.createdAt,
                now
        );
    }
}