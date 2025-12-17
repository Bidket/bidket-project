package com.bidket.auction.domain.outbox.repository;

import com.bidket.auction.domain.outbox.model.AuctionOutbox;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository {

    AuctionOutbox save(AuctionOutbox outbox);

    List<AuctionOutbox> findReadyToPublish(int batchSize);

    Optional<AuctionOutbox> findById(UUID id);
}
