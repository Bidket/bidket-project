package com.bidket.auction.infrastructure.compensation.persistence;

import com.bidket.auction.domain.compensation.model.CompensationLog;
import com.bidket.auction.domain.compensation.model.CompensationStatus;
import com.bidket.auction.domain.compensation.repository.CompensationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CompensationLogRepository 구현체
 */
@Repository
@RequiredArgsConstructor
public class CompensationLogRepositoryImpl implements CompensationLogRepository {

    private final CompensationLogJpaRepository jpaRepository;

    @Override
    public CompensationLog save(CompensationLog compensationLog) {
        return jpaRepository.save(compensationLog);
    }

    @Override
    public Optional<CompensationLog> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<CompensationLog> findBySagaIdOrderByStepNumberDesc(UUID sagaId) {
        return jpaRepository.findBySagaIdOrderByStepNumberDesc(sagaId);
    }

    @Override
    public List<CompensationLog> findBySagaIdAndStatus(UUID sagaId, CompensationStatus status) {
        return jpaRepository.findBySagaIdAndStatus(sagaId, status);
    }

    @Override
    public List<CompensationLog> findRetryableCompensations() {
        return jpaRepository.findRetryableCompensations();
    }

    @Override
    public long countPendingCompensationsBySagaId(UUID sagaId) {
        return jpaRepository.countPendingCompensationsBySagaId(sagaId);
    }

    @Override
    public List<CompensationLog> saveAll(List<CompensationLog> compensationLogs) {
        return jpaRepository.saveAll(compensationLogs);
    }
}
