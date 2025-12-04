package com.bidket.user.infrastructure.persistence.repository;

import com.bidket.user.infrastructure.persistence.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 리프레시 토큰 Repository
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    /** 사용자 ID로 리프레시 토큰 조회 */
    Optional<RefreshToken> findByUserId(UUID userId);
    
    /** 토큰 문자열로 리프레시 토큰 조회 */
    Optional<RefreshToken> findByToken(String token);
    
    /** 사용자 ID로 리프레시 토큰 삭제 */
    void deleteByUserId(UUID userId);
}

