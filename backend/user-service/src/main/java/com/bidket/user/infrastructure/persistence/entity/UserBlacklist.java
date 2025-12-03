package com.bidket.user.infrastructure.persistence.entity;

import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_user_blacklist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlacklist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Builder
    public UserBlacklist(UUID userId, String reason, LocalDateTime expireAt, Boolean active) {
        this.userId = userId;
        this.reason = reason;
        this.expireAt = expireAt;
        this.active = active != null ? active : true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public boolean isExpired() {
        if (this.expireAt == null) {
            return false; // 무기한 블랙리스트
        }
        return LocalDateTime.now().isAfter(this.expireAt);
    }

    public boolean isValid() {
        return this.active && !isExpired();
    }
}

