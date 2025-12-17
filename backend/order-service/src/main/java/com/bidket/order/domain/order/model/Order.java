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

    /**
     * 결제 완료로 상태 변경
     * PAYMENT → PAID
     */
    public Order markPaid(LocalDateTime now) {
        if (status != OrderStatus.PAYMENT) {
            throw new IllegalStateException("결제 대기 상태만 결제 완료로 변경할 수 있습니다. 현재 상태: " + status);
        }
        return new Order(
                id,
                userId,
                auctionId,
                shoeId,
                OrderStatus.PAID,
                amount,
                usedPointAmount,
                paymentExpiredAt,
                createdAt,
                now
        );
    }

    /**
     * 결제 시간 초과로 상태 변경
     * PAYMENT → EXPIRED
     */
    public Order expire(LocalDateTime now) {
        if (status != OrderStatus.PAYMENT) {
            throw new IllegalStateException("결제 대기 상태만 만료 처리할 수 있습니다. 현재 상태: " + status);
        }
        return new Order(
                id,
                userId,
                auctionId,
                shoeId,
                OrderStatus.EXPIRED,
                amount,
                usedPointAmount,
                paymentExpiredAt,
                createdAt,
                now
        );
    }

    /**
     * 주문 취소
     * PAYMENT → CANCELED
     */
    public Order cancel(LocalDateTime now) {
        if (status != OrderStatus.PAYMENT) {
            throw new IllegalStateException("결제 대기 상태만 취소할 수 있습니다. 현재 상태: " + status);
        }
        return new Order(
                id,
                userId,
                auctionId,
                shoeId,
                OrderStatus.CANCELED,
                amount,
                usedPointAmount,
                paymentExpiredAt,
                createdAt,
                now
        );
    }
}