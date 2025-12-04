package com.bidket.auction.domain.bid.model;

public enum BidStatus {

    PENDING("입찰 요청 수신, 검증 대기"),

    ACTIVE("유효한 입찰 (최고가 또는 대기)"),

    OUTBID("다른 입찰에 밀림"),

    WON("경매 낙찰"),

    CANCELLED("사용자/시스템 취소"),

    REJECTED("검증 실패 (금액 부족 등)"),

    PAYMENT_PENDING("결제 대기 중"),

    COMPLETED("결제 완료"),

    PAYMENT_FAILED("결제 실패/타임아웃");

    private final String description;

    BidStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}


