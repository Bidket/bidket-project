package com.bidket.auction.domain.auction.model;

public enum AuctionStatus {
    CREATING("생성 중"),
    PENDING("대기 중"),
    ACTIVE("진행 중"),
    SUCCESS("낙찰 완료"),
    EXPIRED("유찰"),
    CANCELLED("취소됨"),
    PAYMENT_PENDING("결제 대기 중"),
    REOPENED("재오픈됨"),
    COMPLETED("최종 완료");

    private final String description;

    AuctionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}


