package com.bidket.auction.infrastructure.outbox.persistence;

import com.bidket.auction.domain.outbox.model.AuctionOutbox;
import com.bidket.auction.domain.outbox.model.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuctionOutboxJpaRepository extends JpaRepository<AuctionOutbox, UUID> {

    List<AuctionOutbox> findByStatusIn(List<OutboxStatus> statuses, Pageable pageable);

    boolean existsByStatusIn(List<OutboxStatus> statuses);
}
