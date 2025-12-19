package com.bidket.notification.domain.model;

/**
 * 알림 카테고리
 * Kafka topic 기반 카테고리
 */
public enum NotificationCategory {
    QUEUE,              // 대기 순번 관련 (notification.queue.*)
    AUCTION,            // 경매 관련 (notification.auction.*)
    ORDER               // 주문 관련 (notification.order.*)
}

