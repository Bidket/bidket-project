package com.bidket.auction.domain.saga.repository;

import com.bidket.auction.domain.saga.model.AuctionEndSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionEndSagaContextRepository {

    AuctionEndSagaContext save(AuctionEndSagaContext context);

    Optional<AuctionEndSagaContext> findById(UUID sagaId);

    Optional<AuctionEndSagaContext> findByAuctionId(UUID auctionId);

    List<AuctionEndSagaContext> findByStatus(SagaStatus status);
}
