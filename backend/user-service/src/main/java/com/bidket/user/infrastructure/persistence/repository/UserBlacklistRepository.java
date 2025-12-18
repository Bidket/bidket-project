package com.bidket.user.infrastructure.persistence.repository;

import com.bidket.user.infrastructure.persistence.entity.UserBlacklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * @param userId 사용자 ID
     * @return 활성화되고 만료되지 않은 블랙리스트 (active=true AND (expire_at IS NULL OR expire_at > now()))
     */
    @Query("SELECT ub FROM UserBlacklist ub WHERE ub.userId = :userId " +
           "AND ub.active = true " +
           "AND (ub.expireAt IS NULL OR ub.expireAt > :now)")
    Optional<UserBlacklist> findActiveBlacklistByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    /**
     * 사용자의 최근 블랙리스트 조회 (활성화 여부와 무관)
     * @param userId 사용자 ID
     * @return 사용자의 가장 최근 블랙리스트 (활성화 여부와 무관)
     */
    Optional<UserBlacklist> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * 블랙리스트 목록 조회 (페이징, 활성화 여부 필터링)
     * @param activeOnly 활성화된 블랙리스트만 조회할지 여부
     * @param now 현재 시간 (만료 여부 확인용)
     * @param pageable 페이지네이션 정보
     * @return 블랙리스트 페이지
     */
    @Query("SELECT ub FROM UserBlacklist ub WHERE " +
           "(:activeOnly = false OR (ub.active = true AND (ub.expireAt IS NULL OR ub.expireAt > :now))) " +
           "ORDER BY ub.createdAt DESC")
    Page<UserBlacklist> findAllWithFilter(@Param("activeOnly") boolean activeOnly, 
                                          @Param("now") LocalDateTime now, 
                                          Pageable pageable);
}

