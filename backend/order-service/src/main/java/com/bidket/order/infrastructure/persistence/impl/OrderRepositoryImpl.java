package com.bidket.order.infrastructure.persistence.impl;

import com.bidket.order.domain.model.Order;
import com.bidket.order.domain.repository.OrderRepository;
import com.bidket.order.infrastructure.persistence.entity.OrderEntity;
import com.bidket.order.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        OrderEntity saved = orderJpaRepository.save(entity);
        return toDomain(saved);
    }

    private OrderEntity toEntity(Order order) {
        return OrderEntity.create(
                order.getUserId(),
                order.getAuctionId(),
                order.getShoeId(),
                order.getStatus(),
                order.getAmount(),
                order.getUsedPointAmount(),
                order.getPaymentExpiredAt()
        );
    }

    private Order toDomain(OrderEntity entity) {
        return Order.of(
                entity.getId(),
                entity.getUserId(),
                entity.getAuctionId(),
                entity.getShoeId(),
                entity.getStatus(),
                entity.getAmount(),
                entity.getUsedPointAmount(),
                entity.getPaymentExpiredAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}