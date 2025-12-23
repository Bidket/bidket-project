package com.bidket.auction.domain.compensation.model;

public enum CompensationType {
     
    CANCEL_ORDER("주문 취소"),

    REVERT_BID_STATUS("입찰 상태 복원"),

    REOPEN_AUCTION("경매 재오픈"),

    CANCEL_PAYMENT("결제 취소");

    private final String description;

    CompensationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
