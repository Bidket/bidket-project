package com.bidket.user.infrastructure.persistence.repository;

import com.bidket.user.infrastructure.persistence.entity.UserBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 블랙리스트 Repository
 */
@Repository
public interface UserBlacklistRepository extends JpaRepository<UserBlacklist, Long> {
    
    /**
     * 활성화된 블랙리스트 조회
     * 
     * @param userId 사용자 ID
     * @return 활성화되고 만료되지 않은 블랙리스트 (active=true AND (expire_at IS NULL OR expire_at > now()))
     */
    @Query("SELECT ub FROM UserBlacklist ub WHERE ub.userId = :userId " +
           "AND ub.active = true " +
           "AND (ub.expireAt IS NULL OR ub.expireAt > :now)")
    Optional<UserBlacklist> findActiveBlacklistByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}

