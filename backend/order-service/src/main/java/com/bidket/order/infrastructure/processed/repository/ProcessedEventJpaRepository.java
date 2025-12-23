package com.bidket.order.infrastructure.processed.repository;

import com.bidket.order.infrastructure.processed.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 처리된 이벤트 JPA 저장소
 */
public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, UUID> {
}
