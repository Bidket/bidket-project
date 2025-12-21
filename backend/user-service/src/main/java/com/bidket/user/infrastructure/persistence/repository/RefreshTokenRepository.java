package com.bidket.user.infrastructure.persistence.repository;

import com.bidket.user.infrastructure.persistence.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 리프레시 토큰 Repository
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    /** 사용자 ID로 모든 리프레시 토큰 조회 */
    List<RefreshToken> findAllByUserId(UUID userId);
    
    /** 토큰 문자열로 리프레시 토큰 조회 */
    Optional<RefreshToken> findByToken(String token);
    
    /**
     * 사용자 ID로 리프레시 토큰 삭제
     * @Modifying과 @Query를 사용하여 명시적인 삭제 쿼리로 실행되도록 보장
     * 서비스 메서드에 @Transactional이 있어야 정상 동작
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}

