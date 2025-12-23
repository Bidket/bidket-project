package com.bidket.order.infrastructure.outbox.impl;

import com.bidket.order.domain.outbox.model.OrderOutbox;
import com.bidket.order.domain.outbox.model.OutboxStatus;
import com.bidket.order.domain.outbox.repository.OrderOutboxRepository;
import com.bidket.order.infrastructure.outbox.entity.OrderOutboxEntity;
import com.bidket.order.infrastructure.outbox.repository.OrderOutboxJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/**
 * OrderOutbox Repository 구현체 Entity ↔ Domain 매핑
 */
@Repository
@RequiredArgsConstructor
public class OrderOutboxRepositoryImpl implements OrderOutboxRepository {

    private static final Sort SORT_BY_CREATED_AT_ASC = Sort.by(Sort.Direction.ASC, "createdAt");

    private final OrderOutboxJpaRepository jpaRepository;

    @Override
    public OrderOutbox save(OrderOutbox outbox) {
        // Spring Data JPA가 version=null 인 엔티티를 "new"로 판단하면 persist를 호출하는데,
        // UUID(id)가 이미 존재하는 엔티티에 persist가 호출되면 Hibernate가 detached entity 오류를 발생시킬 수 있음.
        // 따라서 기존 row가 있으면 version을 포함해서 merge(save)되도록 만든다.
        Long version = null;
        if (outbox.id() != null) {
            version = jpaRepository.findById(outbox.id())
                    .map(OrderOutboxEntity::getVersion)
                    .orElse(null);
        }

        OrderOutboxEntity entity = toEntity(outbox, version);
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
        return toEntity(domain, null);
    }

    /**
     * Domain → Entity 변환 (version 포함)
     */
    private OrderOutboxEntity toEntity(OrderOutbox domain, Long version) {
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
                version
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
