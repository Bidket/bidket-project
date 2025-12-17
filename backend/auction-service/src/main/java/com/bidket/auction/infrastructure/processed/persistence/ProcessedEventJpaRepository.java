package com.bidket.auction.infrastructure.processed.persistence;

import com.bidket.auction.domain.processed.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEvent, UUID> {
}
