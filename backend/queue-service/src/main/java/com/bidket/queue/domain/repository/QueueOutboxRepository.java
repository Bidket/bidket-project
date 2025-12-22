package com.bidket.queue.domain.repository;

import com.bidket.queue.domain.model.outbox.OutboxStatus;
import com.bidket.queue.infrastructure.persistence.entity.QueueOutboxEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface QueueOutboxRepository extends ReactiveCrudRepository<QueueOutboxEntity, UUID> {
    Flux<QueueOutboxEntity> findAllByStatus(OutboxStatus status);

    Flux<QueueOutboxEntity> findAllByStatusAndRetryCountLessThan(OutboxStatus status, int retryCountIsLessThan);
}
