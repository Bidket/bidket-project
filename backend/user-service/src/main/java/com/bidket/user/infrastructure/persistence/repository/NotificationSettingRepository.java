package com.bidket.user.infrastructure.persistence.repository;

import com.bidket.user.infrastructure.persistence.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 알림 설정 Repository
 */
@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, UUID> {
    /** 사용자 ID로 알림 설정 조회 */
    Optional<NotificationSetting> findByUserId(UUID userId);
}

