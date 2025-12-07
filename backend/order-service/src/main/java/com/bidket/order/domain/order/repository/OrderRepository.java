package com.bidket.order.domain.order.repository;

import com.bidket.order.domain.order.model.Order;

public interface OrderRepository {

    Order save(Order order);
}