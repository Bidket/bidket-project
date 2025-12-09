package com.bidket.notification.infrastructure.persistence.repository;

import com.bidket.notification.infrastructure.persistence.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 알림 수신 설정 Repository
 */
@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, UUID> {

    /**
     * 사용자 ID로 알림 설정 조회
     */
    Optional<NotificationSetting> findByUserId(UUID userId);

    /**
     * 사용자 ID로 알림 설정 존재 여부 확인
     */
    boolean existsByUserId(UUID userId);
}

