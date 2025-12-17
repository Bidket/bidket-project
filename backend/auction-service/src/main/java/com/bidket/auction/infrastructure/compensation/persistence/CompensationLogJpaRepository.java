package com.bidket.auction.infrastructure.compensation.persistence;

import com.bidket.auction.domain.compensation.model.CompensationLog;
import com.bidket.auction.domain.compensation.model.CompensationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * CompensationLog JPA Repository
 */
public interface CompensationLogJpaRepository extends JpaRepository<CompensationLog, UUID> {

    /**
     * Saga ID로 조회 (stepNumber 역순 정렬)
     */
    List<CompensationLog> findBySagaIdOrderByStepNumberDesc(UUID sagaId);

    /**
     * Saga ID와 상태로 조회
     */
    List<CompensationLog> findBySagaIdAndStatus(UUID sagaId, CompensationStatus status);

    /**
     * 재시도 가능한 보상 로그 조회
     */
    @Query("SELECT c FROM CompensationLog c " +
           "WHERE (c.status = 'PENDING' OR c.status = 'FAILED') " +
           "AND c.retryCount < c.maxRetries " +
           "ORDER BY c.createdAt ASC")
    List<CompensationLog> findRetryableCompensations();

    /**
     * Saga ID로 미완료 보상 개수 조회
     */
    @Query("SELECT COUNT(c) FROM CompensationLog c " +
           "WHERE c.sagaId = :sagaId " +
           "AND c.status IN ('PENDING', 'IN_PROGRESS', 'FAILED')")
    long countPendingCompensationsBySagaId(@Param("sagaId") UUID sagaId);
}
