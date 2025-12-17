package com.bidket.auction.domain.compensation.repository;

import com.bidket.auction.domain.compensation.model.CompensationLog;
import com.bidket.auction.domain.compensation.model.CompensationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 보상 로그 Repository 인터페이스
 */
public interface CompensationLogRepository {

    /**
     * 보상 로그 저장
     */
    CompensationLog save(CompensationLog compensationLog);

    /**
     * ID로 조회
     */
    Optional<CompensationLog> findById(UUID id);

    /**
     * Saga ID로 모든 보상 로그 조회 (stepNumber 역순 정렬)
     * 역순 보상 실행을 위해 사용
     *
     * @param sagaId Saga ID
     * @return 보상 로그 리스트 (stepNumber DESC)
     */
    List<CompensationLog> findBySagaIdOrderByStepNumberDesc(UUID sagaId);

    /**
     * Saga ID와 상태로 조회
     *
     * @param sagaId Saga ID
     * @param status 보상 상태
     * @return 해당 상태의 보상 로그 리스트
     */
    List<CompensationLog> findBySagaIdAndStatus(UUID sagaId, CompensationStatus status);

    /**
     * 재시도 가능한 보상 로그 조회
     * (status = PENDING or FAILED, retry_count < max_retries)
     *
     * @return 재시도 대상 보상 로그 리스트
     */
    List<CompensationLog> findRetryableCompensations();

    /**
     * Saga ID로 미완료 보상 로그 개수 조회
     *
     * @param sagaId Saga ID
     * @return 미완료 보상 로그 개수
     */
    long countPendingCompensationsBySagaId(UUID sagaId);

    /**
     * 모든 보상 로그 조회 (배치)
     */
    List<CompensationLog> saveAll(List<CompensationLog> compensationLogs);
}
