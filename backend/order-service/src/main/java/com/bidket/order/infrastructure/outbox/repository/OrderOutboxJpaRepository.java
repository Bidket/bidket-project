package com.bidket.order.infrastructure.outbox.repository;

import com.bidket.order.domain.outbox.model.OutboxStatus;
import com.bidket.order.infrastructure.outbox.entity.OrderOutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * OrderOutbox JPA Repository
 */
public interface OrderOutboxJpaRepository extends JpaRepository<OrderOutboxEntity, UUID> {

    /**
     * 상태 리스트로 OutBox 조회
     *
     * @param statuses 상태 리스트 (PENDING, FAILED)
     * @param pageable 페이징 정보
     * @return OrderOutboxEntity 리스트
     */
    List<OrderOutboxEntity> findByStatusIn(List<OutboxStatus> statuses, Pageable pageable);
}
