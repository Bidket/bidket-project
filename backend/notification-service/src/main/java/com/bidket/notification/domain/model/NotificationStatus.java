package com.bidket.notification.domain.model;

/**
 * 알림 발송 상태
 */
public enum NotificationStatus {
    PENDING,  // 발송 대기
    QUEUED,   // 대기열
    SENT,     // 발송 성공
    FAILED,   // 발송 실패
    SKIPPED   // 구독 해지 등으로 인한 스킵
}

