package com.bidket.notification.infrastructure.persistence.repository;

import com.bidket.notification.domain.model.NotificationStatus;
import com.bidket.notification.infrastructure.persistence.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 알림 Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * 사용자 ID로 알림 목록 조회 (최신순)
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * 사용자 ID와 읽음 여부로 알림 목록 조회
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND " +
           "(:read = true AND n.readAt IS NOT NULL) OR (:read = false AND n.readAt IS NULL) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndReadStatus(@Param("userId") UUID userId,
                                                  @Param("read") boolean read,
                                                  Pageable pageable);

    /**
     * 사용자 ID로 미읽음 알림 개수 조회
     */
    long countByUserIdAndReadAtIsNull(UUID userId);

    /**
     * 사용자 ID와 상태로 알림 목록 조회
     */
    List<Notification> findByUserIdAndStatus(UUID userId, NotificationStatus status);

    /**
     * 발송 대기 중인 알림 목록 조회
     */
    List<Notification> findByStatusOrderByCreatedAtAsc(NotificationStatus status);
}

