package com.bidket.order.domain.order.repository;

import com.bidket.order.domain.order.model.Order;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository {

    Order save(Order order);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Optional<Order> findById(UUID orderId);
  
    boolean existsByAuctionId(UUID auctionId);

    void softDelete(UUID orderId, UUID userId);
}