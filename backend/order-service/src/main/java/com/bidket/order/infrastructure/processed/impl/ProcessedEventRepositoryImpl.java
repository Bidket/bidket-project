package com.bidket.order.infrastructure.processed.impl;

import com.bidket.order.domain.processed.model.ProcessedEvent;
import com.bidket.order.domain.processed.repository.ProcessedEventRepository;
import com.bidket.order.infrastructure.processed.entity.ProcessedEventEntity;
import com.bidket.order.infrastructure.processed.repository.ProcessedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * 처리된 이벤트 저장소 구현체
 */
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
        ProcessedEventEntity entity = ProcessedEventEntity.create(
                processedEvent.eventId(),
                processedEvent.eventType(),
                processedEvent.correlationId(),
                processedEvent.processedAt(),
                processedEvent.result()
        );
        ProcessedEventEntity saved = jpaRepository.save(entity);
        return new ProcessedEvent(
                saved.getEventId(),
                saved.getEventType(),
                saved.getCorrelationId(),
                saved.getProcessedAt(),
                saved.getResult()
        );
    }
}
