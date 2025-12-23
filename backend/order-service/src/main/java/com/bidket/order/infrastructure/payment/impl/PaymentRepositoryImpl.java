package com.bidket.order.infrastructure.payment.impl;

import com.bidket.order.domain.payment.model.Payment;
import com.bidket.order.domain.payment.model.PaymentStatus;
import com.bidket.order.domain.payment.repository.PaymentRepository;
import com.bidket.order.infrastructure.payment.entity.PaymentEntity;
import com.bidket.order.infrastructure.payment.repository.PaymentJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = PaymentEntity.from(payment);
        return paymentJpaRepository.save(entity).toModel();
    }

    @Override
    public Page<Payment> findByUserId(UUID userId, Pageable pageable) {
        return paymentJpaRepository.findByUserId(userId, pageable)
                .map(PaymentEntity::toModel);
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return paymentJpaRepository.findById(paymentId)
                .map(PaymentEntity::toModel);
    }

    public Optional<Payment> findByOrderId(UUID orderId) {
        return paymentJpaRepository.findByOrderId(orderId)
                .map(PaymentEntity::toModel);
    }

    @Override
    public Page<Payment> findByUserIdAndStatus(UUID userId, PaymentStatus status,
            Pageable pageable) {
        return paymentJpaRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(PaymentEntity::toModel);
    }

    @Override
    public boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status) {
        return paymentJpaRepository.existsByOrderIdAndStatus(orderId, status);
    }
}