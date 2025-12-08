package com.bidket.notification.infrastructure.persistence.entity;

import com.bidket.common.infra.BaseEntity;
import com.bidket.notification.domain.model.DeviceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 푸시 토큰 엔티티
 */
@Entity
@Table(name = "p_push_token", indexes = {
        @Index(name = "idx_push_user_id", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_push_token_token", columnNames = "token")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(name = "token", nullable = false, length = 512, unique = true)
    private String token;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Builder
    public PushToken(UUID userId, DeviceType deviceType, String token, Boolean isActive) {
        this.userId = userId;
        this.deviceType = deviceType;
        this.token = token;
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * 토큰 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 토큰 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 마지막 사용 시각 업데이트
     */
    public void updateLastUsedAt() {
        this.lastUsedAt = LocalDateTime.now();
    }
}

