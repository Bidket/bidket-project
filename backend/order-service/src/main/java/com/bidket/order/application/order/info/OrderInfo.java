package com.bidket.order.application.order.info;

import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
public class OrderInfo {

    private final UUID orderId;
    private final UUID userId;
    private final UUID auctionId;
    private final UUID shoeId;
    private final OrderStatus status;
    private final Long amount;
    private final Long usedPointAmount;
    private final LocalDateTime paymentExpiredAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public OrderInfo(UUID orderId,
            UUID userId,
            UUID auctionId,
            UUID shoeId,
            OrderStatus status,
            Long amount,
            Long usedPointAmount,
            LocalDateTime paymentExpiredAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.orderId = orderId;
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

    public static OrderInfo from(Order order) {
        return new OrderInfo(
                order.id(),
                order.userId(),
                order.auctionId(),
                order.shoeId(),
                order.status(),
                order.amount(),
                order.usedPointAmount(),
                order.paymentExpiredAt(),
                order.createdAt(),
                order.updatedAt()
        );
    }
}