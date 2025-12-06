package com.bidket.order.infrastructure.persistence.entity;

import com.bidket.common.infra.BaseEntity;
import com.bidket.order.domain.model.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long usedPointAmount;

    @Column(nullable = false)
    private LocalDateTime paymentExpiredAt;

    protected OrderEntity() {
    }

    private OrderEntity(UUID userId,
            UUID auctionId,
            UUID shoeId,
            OrderStatus status,
            Long amount,
            Long usedPointAmount,
            LocalDateTime paymentExpiredAt) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.shoeId = shoeId;
        this.status = status;
        this.amount = amount;
        this.usedPointAmount = usedPointAmount;
        this.paymentExpiredAt = paymentExpiredAt;
    }

    public static OrderEntity create(UUID userId,
            UUID auctionId,
            UUID shoeId,
            OrderStatus status,
            Long amount,
            Long usedPointAmount,
            LocalDateTime paymentExpiredAt) {
        return new OrderEntity(
                userId,
                auctionId,
                shoeId,
                status,
                amount,
                usedPointAmount,
                paymentExpiredAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public UUID getShoeId() {
        return shoeId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Long getAmount() {
        return amount;
    }

    public Long getUsedPointAmount() {
        return usedPointAmount;
    }

    public LocalDateTime getPaymentExpiredAt() {
        return paymentExpiredAt;
    }
}