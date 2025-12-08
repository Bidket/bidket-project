package com.bidket.order.infrastructure.payment.impl;

import com.bidket.order.domain.payment.model.Payment;
import com.bidket.order.domain.payment.repository.PaymentRepository;
import com.bidket.order.infrastructure.payment.entity.PaymentEntity;
import com.bidket.order.infrastructure.payment.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity saved = jpaRepository.save(PaymentEntity.from(payment));
        return saved.toModel();
    }
}