package com.bidket.auction.domain.compensation.model;

public enum CompensationStatus {
     
    PENDING("대기 중"),

    IN_PROGRESS("진행 중"),

    COMPLETED("완료"),

    FAILED("실패");

    private final String description;

    CompensationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    public boolean isRetryable() {
        return this == PENDING || this == FAILED;
    }
}
