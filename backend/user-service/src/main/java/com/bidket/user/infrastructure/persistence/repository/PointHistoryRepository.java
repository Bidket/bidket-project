package com.bidket.user.infrastructure.persistence.repository;

import com.bidket.user.domain.model.PointHistoryType;
import com.bidket.user.infrastructure.persistence.entity.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * 포인트 거래 내역 Repository
 */
@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    /**
     * 사용자 ID로 포인트 거래 내역 조회 (페이지네이션)
     * 최신순으로 정렬
     */
    Page<PointHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    /**
     * 사용자 ID와 타입으로 포인트 거래 내역 조회 (페이지네이션)
     * 최신순으로 정렬
     */
    Page<PointHistory> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, PointHistoryType type, Pageable pageable);
}

