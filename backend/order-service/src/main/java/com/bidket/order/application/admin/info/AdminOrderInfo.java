package com.bidket.order.application.admin.info;

import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminOrderInfo(
        UUID orderId,
        UUID userId,
        UUID auctionId,
        UUID shoeId,
        OrderStatus status,
        Long amount,
        LocalDateTime createdAt
) {

    public static AdminOrderInfo from(Order order) {
        return new AdminOrderInfo(
                order.id(),
                order.userId(),
                order.auctionId(),
                order.shoeId(),
                order.status(),
                order.amount(),
                order.createdAt()
        );
    }
}