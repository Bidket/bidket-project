package com.bidket.order.infrastructure.order.persistence.repository;

import com.bidket.order.infrastructure.order.persistence.entity.OrderEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Page<OrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    boolean existsByAuctionId(UUID auctionId);

    Optional<OrderEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID orderId, UUID userId);

    Page<OrderEntity> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId,
            Pageable pageable);

    boolean existsByAuctionIdAndDeletedAtIsNull(UUID auctionId);
}