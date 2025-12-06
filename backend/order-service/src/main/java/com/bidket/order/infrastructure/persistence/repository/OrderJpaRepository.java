package com.bidket.order.infrastructure.persistence.repository;

import com.bidket.order.infrastructure.persistence.entity.OrderEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

}