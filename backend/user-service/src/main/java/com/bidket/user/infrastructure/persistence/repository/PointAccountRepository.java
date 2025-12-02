package com.bidket.user.infrastructure.persistence.repository;

import com.bidket.user.infrastructure.persistence.entity.PointAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 포인트 계정 Repository
 */
@Repository
public interface PointAccountRepository extends JpaRepository<PointAccount, UUID> {
    /** 사용자 ID로 포인트 계정 조회 */
    Optional<PointAccount> findByUserId(UUID userId);
}

