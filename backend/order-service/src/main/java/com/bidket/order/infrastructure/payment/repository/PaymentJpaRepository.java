package com.bidket.order.infrastructure.payment.repository;

import com.bidket.order.infrastructure.payment.entity.PaymentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

}