package com.bidket.auction.infrastructure.saga.persistence;

import com.bidket.auction.domain.saga.model.AuctionEndSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionEndSagaContextJpaRepository extends JpaRepository<AuctionEndSagaContext, UUID> {

    Optional<AuctionEndSagaContext> findByAuctionId(UUID auctionId);

    List<AuctionEndSagaContext> findByStatus(SagaStatus status);
}
