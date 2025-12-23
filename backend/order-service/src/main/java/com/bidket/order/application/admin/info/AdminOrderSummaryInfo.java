package com.bidket.order.application.admin.info;

import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminOrderSummaryInfo(
        UUID orderId,
        UUID userId,
        OrderStatus status,
        Long amount,
        LocalDateTime createdAt
) {

    public static AdminOrderSummaryInfo from(Order order) {
        return new AdminOrderSummaryInfo(
                order.id(),
                order.userId(),
                order.status(),
                order.amount(),
                order.createdAt()
        );
    }
}