package com.bidket.order.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Order {

    private final UUID id;
    private final UUID userId;
    private final UUID auctionId;
    private final UUID shoeId;
    private final OrderStatus status;
    private final Long amount;
    private final Long usedPointAmount;
    private final LocalDateTime paymentExpiredAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Order(UUID id,
            UUID userId,
            UUID auctionId,
            UUID shoeId,
            OrderStatus status,
            Long amount,
            Long usedPointAmount,
            LocalDateTime paymentExpiredAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.auctionId = auctionId;
        this.shoeId = shoeId;
        this.status = status;
        this.amount = amount;
        this.usedPointAmount = usedPointAmount;
        this.paymentExpiredAt = paymentExpiredAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order createForPayment(UUID userId,
            UUID auctionId,
            UUID shoeId,
            Long amount,
            Long usedPointAmount,
            LocalDateTime paymentExpiredAt,
            LocalDateTime now) {

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

        return new Order(
                null,
                userId,
                auctionId,
                shoeId,
                OrderStatus.PAYMENT,
                amount,
                safeUsedPoint,
                paymentExpiredAt,
                now,
                now
        );
    }

    public static Order of(UUID id,
            UUID userId,
            UUID auctionId,
            UUID shoeId,
            OrderStatus status,
            Long amount,
            Long usedPointAmount,
            LocalDateTime paymentExpiredAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

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