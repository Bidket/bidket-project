package com.bidket.order.domain.repository;

import com.bidket.order.domain.model.Order;

public interface OrderRepository {

    Order save(Order order);
}