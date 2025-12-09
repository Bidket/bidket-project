package com.bidket.order.infrastructure.order.persistence.impl;

import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.repository.OrderRepository;
import com.bidket.order.infrastructure.order.persistence.entity.OrderEntity;
import com.bidket.order.infrastructure.order.persistence.repository.OrderJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Override
    public Page<Order> findByUserId(UUID userId, Pageable pageable) {
        Page<OrderEntity> page = orderJpaRepository.findByUserIdOrderByCreatedAtDesc(userId,
                pageable);
        return page.map(this::toDomain);
    }

    private OrderEntity toEntity(Order order) {
        return OrderEntity.create(
                order.userId(),
                order.auctionId(),
                order.shoeId(),
                order.status(),
                order.amount(),
                order.usedPointAmount(),
                order.paymentExpiredAt()
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