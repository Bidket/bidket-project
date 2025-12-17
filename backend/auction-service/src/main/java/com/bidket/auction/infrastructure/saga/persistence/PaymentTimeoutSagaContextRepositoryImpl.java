package com.bidket.auction.infrastructure.saga.persistence;

import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;
import com.bidket.auction.domain.saga.repository.PaymentTimeoutSagaContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PaymentTimeoutSagaContext Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class PaymentTimeoutSagaContextRepositoryImpl implements PaymentTimeoutSagaContextRepository {

    private final PaymentTimeoutSagaContextJpaRepository jpaRepository;

    @Override
    public PaymentTimeoutSagaContext save(PaymentTimeoutSagaContext context) {
        return jpaRepository.save(context);
    }

    @Override
    public Optional<PaymentTimeoutSagaContext> findById(UUID sagaId) {
        return jpaRepository.findById(sagaId);
    }

    @Override
    public Optional<PaymentTimeoutSagaContext> findLatestByAuctionId(UUID auctionId) {
        return jpaRepository.findLatestByAuctionId(auctionId);
    }

    @Override
    public Optional<PaymentTimeoutSagaContext> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId);
    }

    @Override
    public List<PaymentTimeoutSagaContext> findByStatus(SagaStatus status) {
        return jpaRepository.findByStatus(status);
    }
}
