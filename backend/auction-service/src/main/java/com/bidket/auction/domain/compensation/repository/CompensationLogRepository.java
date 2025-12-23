package com.bidket.auction.domain.compensation.repository;

import com.bidket.auction.domain.compensation.model.CompensationLog;
import com.bidket.auction.domain.compensation.model.CompensationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompensationLogRepository {

    CompensationLog save(CompensationLog compensationLog);

    Optional<CompensationLog> findById(UUID id);

    List<CompensationLog> findBySagaIdOrderByStepNumberDesc(UUID sagaId);

    List<CompensationLog> findBySagaIdAndStatus(UUID sagaId, CompensationStatus status);

    List<CompensationLog> findRetryableCompensations();

    long countPendingCompensationsBySagaId(UUID sagaId);

    List<CompensationLog> saveAll(List<CompensationLog> compensationLogs);
}
