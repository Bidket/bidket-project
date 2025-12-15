package com.bidket.auction.infrastructure.outbox.persistence;

import com.bidket.auction.domain.outbox.model.AuctionOutbox;
import com.bidket.auction.domain.outbox.model.OutboxStatus;
import com.bidket.auction.domain.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxRepositoryImpl implements OutboxRepository {

    private static final Sort SORT_BY_CREATED_AT_ASC = Sort.by(Sort.Direction.ASC, "createdAt");

    private final AuctionOutboxJpaRepository jpaRepository;

    @Override
    public AuctionOutbox save(AuctionOutbox outbox) {
        return jpaRepository.save(outbox);
    }

    @Override
    public List<AuctionOutbox> findReadyToPublish(int batchSize) {
        Pageable pageable = PageRequest.of(0, batchSize, SORT_BY_CREATED_AT_ASC);
        return jpaRepository.findByStatusIn(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                pageable
        );
    }

    @Override
    public Optional<AuctionOutbox> findById(UUID id) {
        return jpaRepository.findById(id);
    }
}
