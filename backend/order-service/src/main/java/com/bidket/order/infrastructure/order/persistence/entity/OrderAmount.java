package com.bidket.order.infrastructure.order.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class OrderAmount {

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "used_point_amount", nullable = false)
    private Long usedPointAmount;

    protected OrderAmount() {
    }

    private OrderAmount(Long amount, Long usedPointAmount) {
        this.amount = amount;
        this.usedPointAmount = usedPointAmount == null ? 0L : usedPointAmount;
    }

    public static OrderAmount of(Long amount, Long usedPointAmount) {
        return new OrderAmount(amount, usedPointAmount);
    }
}