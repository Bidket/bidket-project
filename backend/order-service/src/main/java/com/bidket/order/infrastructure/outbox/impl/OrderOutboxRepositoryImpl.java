package com.bidket.order.infrastructure.outbox.impl;

import com.bidket.order.domain.outbox.model.OrderOutbox;
import com.bidket.order.domain.outbox.model.OutboxStatus;
import com.bidket.order.domain.outbox.repository.OrderOutboxRepository;
import com.bidket.order.infrastructure.outbox.entity.OrderOutboxEntity;
import com.bidket.order.infrastructure.outbox.repository.OrderOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OrderOutbox Repository 구현체
 * Entity ↔ Domain 매핑
 */
@Repository
@RequiredArgsConstructor
public class OrderOutboxRepositoryImpl implements OrderOutboxRepository {

    private static final Sort SORT_BY_CREATED_AT_ASC = Sort.by(Sort.Direction.ASC, "createdAt");

    private final OrderOutboxJpaRepository jpaRepository;

    @Override
    public OrderOutbox save(OrderOutbox outbox) {
        OrderOutboxEntity entity = toEntity(outbox);
        OrderOutboxEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<OrderOutbox> findReadyToPublish(int batchSize) {
        Pageable pageable = PageRequest.of(0, batchSize, SORT_BY_CREATED_AT_ASC);
        List<OrderOutboxEntity> entities = jpaRepository.findByStatusIn(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                pageable
        );
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<OrderOutbox> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(this::toDomain);
    }

    /**
     * Domain → Entity 변환
     */
    private OrderOutboxEntity toEntity(OrderOutbox domain) {
        return new OrderOutboxEntity(
                domain.id(),
                domain.aggregateType(),
                domain.aggregateId(),
                domain.eventType(),
                domain.correlationId(),
                domain.payload(),
                domain.status(),
                domain.retryCount(),
                domain.errorMessage(),
                domain.publishedAt(),
                null  // version은 JPA가 관리
        );
    }

    /**
     * Entity → Domain 변환
     */
    private OrderOutbox toDomain(OrderOutboxEntity entity) {
        return new OrderOutbox(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getCorrelationId(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getErrorMessage(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
