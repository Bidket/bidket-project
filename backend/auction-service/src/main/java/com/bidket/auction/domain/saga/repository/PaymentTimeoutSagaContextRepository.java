package com.bidket.auction.domain.saga.repository;

import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTimeoutSagaContextRepository {

    PaymentTimeoutSagaContext save(PaymentTimeoutSagaContext context);

    Optional<PaymentTimeoutSagaContext> findById(UUID sagaId);

    Optional<PaymentTimeoutSagaContext> findLatestByAuctionId(UUID auctionId);

    Optional<PaymentTimeoutSagaContext> findByOrderId(UUID orderId);

    List<PaymentTimeoutSagaContext> findByStatus(SagaStatus status);
}
