package com.bidket.auction.domain.compensation.model;

/**
 * 보상 트랜잭션 상태
 * ERD.md line 322 참조
 */
public enum CompensationStatus {
    /**
     * 대기 중
     */
    PENDING("대기 중"),

    /**
     * 진행 중
     */
    IN_PROGRESS("진행 중"),

    /**
     * 완료
     */
    COMPLETED("완료"),

    /**
     * 실패
     */
    FAILED("실패");

    private final String description;

    CompensationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 완료된 상태인지 확인
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    /**
     * 재시도 가능한 상태인지 확인
     */
    public boolean isRetryable() {
        return this == PENDING || this == FAILED;
    }
}
