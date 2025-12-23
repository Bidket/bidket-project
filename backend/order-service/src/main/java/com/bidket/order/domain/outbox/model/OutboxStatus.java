package com.bidket.order.domain.outbox.model;

/**
 * OutBox 상태
 */
public enum OutboxStatus {
    /**
     * 발행 대기 중
     */
    PENDING,

    /**
     * 발행 중 (Optimistic Lock 방지용)
     */
    PUBLISHING,

    /**
     * 발행 완료
     */
    PUBLISHED,

    /**
     * 발행 실패 (재시도 예정)
     */
    FAILED
}
