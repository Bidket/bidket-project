package com.bidket.auction.domain.saga.model;

/**
 * Saga 실행 상태
 * MVP 버전: PENDING, IN_PROGRESS, COMPLETED, COMPENSATING, COMPENSATED, FAILED만 사용
 */
public enum SagaStatus {
    PENDING("대기 중"),
    IN_PROGRESS("진행 중"),
    COMPLETED("완료"),
    COMPENSATING("보상 진행 중"),
    COMPENSATED("보상 완료"),
    FAILED("실패");

    private final String description;

    SagaStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
