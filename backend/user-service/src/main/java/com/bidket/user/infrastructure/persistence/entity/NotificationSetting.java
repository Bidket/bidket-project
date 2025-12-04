package com.bidket.user.infrastructure.persistence.entity;

import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 알림 설정 엔티티
 * 
 * 사용자의 알림 수신 설정을 관리합니다.
 * - allow_push/email/sms: 기본값 true
 * - allow_marketing: 마케팅 알림 수신 동의 여부
 */
@Entity
@Table(name = "p_notification_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseEntity {

    @Id
    @Column(name = "user_id", columnDefinition = "UUID")
    private UUID userId;

    /** 푸시 알림 수신 허용 여부 (기본값: true) */
    @Column(name = "allow_push", nullable = false)
    private Boolean allowPush;

    /** 이메일 알림 수신 허용 여부 (기본값: true) */
    @Column(name = "allow_email", nullable = false)
    private Boolean allowEmail;

    /** SMS 알림 수신 허용 여부 (기본값: true) */
    @Column(name = "allow_sms", nullable = false)
    private Boolean allowSms;

    /** 마케팅 알림 수신 허용 여부 (기본값: false) */
    @Column(name = "allow_marketing", nullable = false)
    private Boolean allowMarketing;

    @Builder
    public NotificationSetting(UUID userId, Boolean allowPush, Boolean allowEmail, 
                              Boolean allowSms, Boolean allowMarketing) {
        this.userId = userId;
        this.allowPush = allowPush != null ? allowPush : true;
        this.allowEmail = allowEmail != null ? allowEmail : true;
        this.allowSms = allowSms != null ? allowSms : true;
        this.allowMarketing = allowMarketing != null ? allowMarketing : false;
    }

    /** 푸시 알림 설정 업데이트 */
    public void updatePushSetting(Boolean allowPush) {
        this.allowPush = allowPush;
    }

    /** 이메일 알림 설정 업데이트 */
    public void updateEmailSetting(Boolean allowEmail) {
        this.allowEmail = allowEmail;
    }

    /** SMS 알림 설정 업데이트 */
    public void updateSmsSetting(Boolean allowSms) {
        this.allowSms = allowSms;
    }

    /** 마케팅 알림 설정 업데이트 */
    public void updateMarketingSetting(Boolean allowMarketing) {
        this.allowMarketing = allowMarketing;
    }
}

