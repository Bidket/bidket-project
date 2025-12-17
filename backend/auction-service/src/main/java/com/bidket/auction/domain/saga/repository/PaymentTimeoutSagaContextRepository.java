package com.bidket.auction.domain.saga.repository;

import com.bidket.auction.domain.saga.model.PaymentTimeoutSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PaymentTimeoutSagaContext Repository 인터페이스
 */
public interface PaymentTimeoutSagaContextRepository {

    /**
     * Saga Context 저장
     */
    PaymentTimeoutSagaContext save(PaymentTimeoutSagaContext context);

    /**
     * Saga Context 조회
     */
    Optional<PaymentTimeoutSagaContext> findById(UUID sagaId);

    /**
     * 경매 ID로 최신 Saga Context 조회
     */
    Optional<PaymentTimeoutSagaContext> findLatestByAuctionId(UUID auctionId);

    /**
     * 주문 ID로 Saga Context 조회
     */
    Optional<PaymentTimeoutSagaContext> findByOrderId(UUID orderId);

    /**
     * 상태별 조회 (복구 시 사용)
     */
    List<PaymentTimeoutSagaContext> findByStatus(SagaStatus status);
}
