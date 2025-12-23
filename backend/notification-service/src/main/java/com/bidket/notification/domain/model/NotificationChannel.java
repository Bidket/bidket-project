package com.bidket.notification.domain.model;

/**
 * 알림 발송 채널
 */
public enum NotificationChannel {
    PUSH,    // 푸시 알림
    EMAIL,   // 이메일
    SMS,     // SMS
    SLACK,   // Slack
    IN_APP   // 인앱 알림
}

