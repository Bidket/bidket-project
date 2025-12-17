package com.bidket.auction.domain.saga.repository;

import com.bidket.auction.domain.saga.model.AuctionEndSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 경매 종료 Saga Context Repository 인터페이스
 */
public interface AuctionEndSagaContextRepository {

    /**
     * Saga Context 저장
     */
    AuctionEndSagaContext save(AuctionEndSagaContext context);

    /**
     * Saga ID로 조회
     */
    Optional<AuctionEndSagaContext> findById(UUID sagaId);

    /**
     * 경매 ID로 조회
     */
    Optional<AuctionEndSagaContext> findByAuctionId(UUID auctionId);

    /**
     * 상태별 조회 (복구 시 사용)
     */
    List<AuctionEndSagaContext> findByStatus(SagaStatus status);
}
