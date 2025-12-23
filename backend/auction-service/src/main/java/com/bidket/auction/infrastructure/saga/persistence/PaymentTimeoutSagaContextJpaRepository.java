package com.bidket.auction.infrastructure.saga.persistence;

import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTimeoutSagaContextJpaRepository extends JpaRepository<PaymentTimeoutSagaContext, UUID> {

    @Query("SELECT s FROM PaymentTimeoutSagaContext s WHERE s.auctionId = :auctionId ORDER BY s.createdAt DESC LIMIT 1")
    Optional<PaymentTimeoutSagaContext> findLatestByAuctionId(@Param("auctionId") UUID auctionId);

    Optional<PaymentTimeoutSagaContext> findByOrderId(UUID orderId);

    List<PaymentTimeoutSagaContext> findByStatus(SagaStatus status);
}
