package com.bidket.order.domain.order.repository;

import com.bidket.order.domain.order.model.Order;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository {

    Order save(Order order);

    Page<Order> findByUserId(UUID userId, Pageable pageable);
}