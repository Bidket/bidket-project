package com.bidket.order.infrastructure.refund.repository;

import com.bidket.order.infrastructure.refund.entity.RefundEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundJpaRepository extends JpaRepository<RefundEntity, UUID> {

}