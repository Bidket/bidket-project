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
}