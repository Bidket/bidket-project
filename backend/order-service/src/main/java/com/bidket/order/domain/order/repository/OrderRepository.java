package com.bidket.order.domain.order.repository;

import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository {

    Order save(Order order);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    Optional<Order> findById(UUID orderId);

    /**
     * 결제 시간이 초과된 주문 조회 - status = PAYMENT - paymentExpiredAt < now
     */
    List<Order> findExpiredOrders(LocalDateTime now);

    boolean existsByAuctionId(UUID auctionId);

    void softDelete(UUID orderId, UUID userId);

    // admin
    Page<Order> findAll(Pageable pageable);

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);
}