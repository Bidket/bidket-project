package com.bidket.auction.infrastructure.compensation.persistence;

import com.bidket.auction.domain.compensation.model.CompensationLog;
import com.bidket.auction.domain.compensation.model.CompensationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CompensationLogJpaRepository extends JpaRepository<CompensationLog, UUID> {

    List<CompensationLog> findBySagaIdOrderByStepNumberDesc(UUID sagaId);

    List<CompensationLog> findBySagaIdAndStatus(UUID sagaId, CompensationStatus status);

    @Query("SELECT c FROM CompensationLog c " +
           "WHERE (c.status = 'PENDING' OR c.status = 'FAILED') " +
           "AND c.retryCount < c.maxRetries " +
           "ORDER BY c.createdAt ASC")
    List<CompensationLog> findRetryableCompensations();

    @Query("SELECT COUNT(c) FROM CompensationLog c " +
           "WHERE c.sagaId = :sagaId " +
           "AND c.status IN ('PENDING', 'IN_PROGRESS', 'FAILED')")
    long countPendingCompensationsBySagaId(@Param("sagaId") UUID sagaId);
}
