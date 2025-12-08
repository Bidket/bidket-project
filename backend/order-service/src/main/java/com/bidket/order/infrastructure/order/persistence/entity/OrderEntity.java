package com.bidket.order.infrastructure.order.persistence.entity;

import com.bidket.common.infra.BaseEntity;
import com.bidket.order.domain.order.model.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(name = "p_order")
public class OrderEntity extends BaseEntity {

    @Id
    @Column(name = "order_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID auctionId;

    @Column(nullable = false)
    private UUID shoeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Embedded
    private OrderAmount orderAmount;

    @Column(nullable = false)
    private LocalDateTime paymentExpiredAt;

    protected OrderEntity() {
    }

    private OrderEntity(
            UUID userId,
            UUID auctionId,
            UUID shoeId,
            OrderStatus status,
            OrderAmount orderAmount,
            LocalDateTime paymentExpiredAt
    ) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.shoeId = shoeId;
        this.status = status;
        this.orderAmount = orderAmount;
        this.paymentExpiredAt = paymentExpiredAt;
    }

    public static OrderEntity create(
            UUID userId,
            UUID auctionId,
            UUID shoeId,
            OrderStatus status,
            Long amount,
            Long usedPointAmount,
            LocalDateTime paymentExpiredAt
    ) {
        return new OrderEntity(
                userId,
                auctionId,
                shoeId,
                status,
                OrderAmount.of(amount, usedPointAmount),
                paymentExpiredAt
        );
    }

    public Long getAmount() {
        return orderAmount != null ? orderAmount.getAmount() : null;
    }

    public Long getUsedPointAmount() {
        return orderAmount != null ? orderAmount.getUsedPointAmount() : null;
    }
}