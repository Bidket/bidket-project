package com.bidket.notification.infrastructure.persistence.entity;

import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 회원별 알림 수신 설정 엔티티
 * 회원 1명당 1행 (1:1 관계)
 */
@Entity
@Table(name = "p_notification_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseEntity {

    @Id
    @Column(name = "user_id", columnDefinition = "UUID")
    private UUID userId;

    @Column(name = "allow_push", nullable = false)
    private Boolean allowPush;

    @Column(name = "allow_email", nullable = false)
    private Boolean allowEmail;

    @Column(name = "allow_sms", nullable = false)
    private Boolean allowSms;

    @Column(name = "allow_marketing", nullable = false)
    private Boolean allowMarketing;

    @Builder
    public NotificationSetting(UUID userId, Boolean allowPush, Boolean allowEmail,
                              Boolean allowSms, Boolean allowMarketing) {
        this.userId = userId;
        this.allowPush = allowPush != null ? allowPush : true;
        this.allowEmail = allowEmail != null ? allowEmail : false;
        this.allowSms = allowSms != null ? allowSms : false;
        this.allowMarketing = allowMarketing != null ? allowMarketing : false;
    }

    /**
     * 푸시 알림 허용 여부 업데이트
     */
    public void updateAllowPush(boolean allowPush) {
        this.allowPush = allowPush;
    }

    /**
     * 이메일 알림 허용 여부 업데이트
     */
    public void updateAllowEmail(boolean allowEmail) {
        this.allowEmail = allowEmail;
    }

    /**
     * SMS 알림 허용 여부 업데이트
     */
    public void updateAllowSms(boolean allowSms) {
        this.allowSms = allowSms;
    }

    /**
     * 마케팅 알림 허용 여부 업데이트
     */
    public void updateAllowMarketing(boolean allowMarketing) {
        this.allowMarketing = allowMarketing;
    }

    /**
     * 모든 설정 업데이트
     */
    public void updateSettings(boolean allowPush, boolean allowEmail,
                              boolean allowSms, boolean allowMarketing) {
        this.allowPush = allowPush;
        this.allowEmail = allowEmail;
        this.allowSms = allowSms;
        this.allowMarketing = allowMarketing;
    }
}

