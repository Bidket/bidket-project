package com.bidket.auction.infrastructure.saga.persistence;

import com.bidket.auction.domain.saga.model.AuctionEndSagaContext;
import com.bidket.auction.domain.saga.model.SagaStatus;
import com.bidket.auction.domain.saga.repository.AuctionEndSagaContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AuctionEndSagaContext Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class AuctionEndSagaContextRepositoryImpl implements AuctionEndSagaContextRepository {

    private final AuctionEndSagaContextJpaRepository jpaRepository;

    @Override
    public AuctionEndSagaContext save(AuctionEndSagaContext context) {
        return jpaRepository.save(context);
    }

    @Override
    public Optional<AuctionEndSagaContext> findById(UUID sagaId) {
        return jpaRepository.findById(sagaId);
    }

    @Override
    public Optional<AuctionEndSagaContext> findByAuctionId(UUID auctionId) {
        return jpaRepository.findByAuctionId(auctionId);
    }

    @Override
    public List<AuctionEndSagaContext> findByStatus(SagaStatus status) {
        return jpaRepository.findByStatus(status);
    }
}
