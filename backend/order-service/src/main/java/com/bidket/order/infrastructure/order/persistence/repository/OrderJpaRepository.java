package com.bidket.order.infrastructure.order.persistence.repository;

import com.bidket.order.domain.order.model.OrderStatus;
import com.bidket.order.infrastructure.order.persistence.entity.OrderEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Page<OrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * 결제 시간이 초과된 주문 조회
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.status = :status AND o.paymentExpiredAt < :now")
    List<OrderEntity> findExpiredOrders(@Param("status") OrderStatus status, @Param("now") LocalDateTime now);

    boolean existsByAuctionId(UUID auctionId);

    Optional<OrderEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID orderId, UUID userId);

    Page<OrderEntity> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId,
            Pageable pageable);

    boolean existsByAuctionIdAndDeletedAtIsNull(UUID auctionId);
}