package com.bidket.auction.domain.processed.repository;

import com.bidket.auction.domain.processed.model.ProcessedEvent;

import java.util.UUID;

public interface ProcessedEventRepository {

    boolean existsById(UUID eventId);

    ProcessedEvent save(ProcessedEvent processedEvent);
}
