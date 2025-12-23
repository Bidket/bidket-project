package com.bidket.order.infrastructure.payment.repository;

import com.bidket.order.domain.payment.model.PaymentStatus;
import com.bidket.order.infrastructure.payment.entity.PaymentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Page<PaymentEntity> findByUserId(UUID userId, Pageable pageable);

    Optional<PaymentEntity> findByOrderId(UUID orderId);

    Page<PaymentEntity> findByUserIdAndStatus(UUID userId, PaymentStatus status, Pageable pageable);

    Optional<PaymentEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);
}