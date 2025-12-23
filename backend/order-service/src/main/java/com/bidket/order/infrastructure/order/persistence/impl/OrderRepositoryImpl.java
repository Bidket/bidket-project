package com.bidket.order.infrastructure.order.persistence.impl;

import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.model.OrderStatus;
import com.bidket.order.domain.order.repository.OrderRepository;
import com.bidket.order.infrastructure.order.persistence.entity.OrderEntity;
import com.bidket.order.infrastructure.order.persistence.repository.OrderJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        OrderEntity entity;
        if (order.id() != null) {
            // 기존 엔티티 업데이트
            entity = orderJpaRepository.findById(order.id())
                    .map(existing -> {
                        existing.setStatus(order.status());
                        return existing;
                    })
                    .orElseGet(() -> toEntity(order));
        } else {
            entity = toEntity(order);
        }
        OrderEntity saved = orderJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Page<Order> findByUserId(UUID userId, Pageable pageable) {
        Page<OrderEntity> page = orderJpaRepository.findByUserIdOrderByCreatedAtDesc(userId,
                pageable);
        return page.map(this::toDomain);
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return orderJpaRepository.findById(orderId)
                .map(this::toDomain);
    }

    @Override
    public List<Order> findExpiredOrders(LocalDateTime now) {
        return orderJpaRepository.findExpiredOrders(OrderStatus.PAYMENT, now)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return orderJpaRepository.findById(orderId).map(this::toDomain);
    }

    @Override
    public boolean existsByAuctionId(UUID auctionId) {
        return orderJpaRepository.existsByAuctionId(auctionId);
    }

    @Override
    @Transactional
    public void softDelete(UUID orderId, UUID userId) {
        OrderEntity entity = orderJpaRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("주문을 찾을 수 없습니다."));

        if (!entity.getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 주문만 삭제할 수 있습니다.");
        }

        if (entity.isDeleted()) {
            return;
        }

        entity.markDeleted(null);
        entity.setDeletedByUuid(userId);

        orderJpaRepository.save(entity);
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