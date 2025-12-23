package com.bidket.order.infrastructure.refund.repository;

import com.bidket.order.infrastructure.refund.entity.RefundEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundJpaRepository extends JpaRepository<RefundEntity, UUID> {

    Page<RefundEntity> findByUserId(UUID userId, Pageable pageable);
}