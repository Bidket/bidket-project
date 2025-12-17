package com.bidket.auction.infrastructure.saga.persistence;

import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PaymentTimeoutSagaContext JPA Repository
 */
public interface PaymentTimeoutSagaContextJpaRepository extends JpaRepository<PaymentTimeoutSagaContext, UUID> {

    /**
     * 경매 ID로 최신 Saga Context 조회 (생성 시간 기준 내림차순)
     */
    @Query("SELECT s FROM PaymentTimeoutSagaContext s WHERE s.auctionId = :auctionId ORDER BY s.createdAt DESC LIMIT 1")
    Optional<PaymentTimeoutSagaContext> findLatestByAuctionId(@Param("auctionId") UUID auctionId);

    /**
     * 주문 ID로 Saga Context 조회
     */
    Optional<PaymentTimeoutSagaContext> findByOrderId(UUID orderId);

    /**
     * 상태별 조회 (복구 시 사용)
     */
    List<PaymentTimeoutSagaContext> findByStatus(SagaStatus status);
}
