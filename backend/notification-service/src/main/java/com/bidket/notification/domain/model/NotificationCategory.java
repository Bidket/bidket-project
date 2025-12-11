package com.bidket.notification.domain.model;

/**
 * 알림 카테고리
 */
public enum NotificationCategory {
    AUCTION_START,      // 경매 시작
    BID_SUCCESS,        // 입찰 성공/최고가 업데이트
    PAYMENT_EXPIRE,     // 결제 만료 알림
    QUEUE_CALL,         // 대기열 호출
    PAYMENT_DONE,       // 결제 완료
    SYSTEM              // 시스템 알림
}

