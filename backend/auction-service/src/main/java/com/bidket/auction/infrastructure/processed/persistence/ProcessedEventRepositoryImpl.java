package com.bidket.auction.infrastructure.processed.persistence;

import com.bidket.auction.domain.processed.model.ProcessedEvent;
import com.bidket.auction.domain.processed.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProcessedEventRepositoryImpl implements ProcessedEventRepository {

    private final ProcessedEventJpaRepository jpaRepository;

    @Override
    public boolean existsById(UUID eventId) {
        return jpaRepository.existsById(eventId);
    }

    @Override
    public ProcessedEvent save(ProcessedEvent processedEvent) {
        return jpaRepository.save(processedEvent);
    }
}
