package com.bidket.notification.infrastructure.persistence.repository;

import com.bidket.notification.domain.model.DeviceType;
import com.bidket.notification.infrastructure.persistence.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 푸시 토큰 Repository
 */
@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    /**
     * 사용자 ID로 활성화된 푸시 토큰 목록 조회
     */
    List<PushToken> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * 사용자 ID로 모든 푸시 토큰 목록 조회
     */
    List<PushToken> findByUserId(UUID userId);

    /**
     * 토큰 값으로 조회
     */
    Optional<PushToken> findByToken(String token);

    /**
     * 사용자 ID와 디바이스 타입으로 활성화된 토큰 조회
     */
    Optional<PushToken> findByUserIdAndDeviceTypeAndIsActiveTrue(UUID userId, DeviceType deviceType);

    /**
     * 사용자 ID와 디바이스 타입으로 모든 토큰 조회
     */
    List<PushToken> findByUserIdAndDeviceType(UUID userId, DeviceType deviceType);
}

