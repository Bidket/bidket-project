package com.bidket.auction.domain.compensation.model;

/**
 * 보상 트랜잭션 타입
 * ERD.md line 321 참조
 */
public enum CompensationType {
    /**
     * 재고 복원
     */
    RESTORE_STOCK("재고 복원"),

    /**
     * 주문 취소
     */
    CANCEL_ORDER("주문 취소"),

    /**
     * 입찰 상태 복원
     */
    REVERT_BID_STATUS("입찰 상태 복원"),

    /**
     * 경매 재오픈
     */
    REOPEN_AUCTION("경매 재오픈"),

    /**
     * 결제 취소
     */
    CANCEL_PAYMENT("결제 취소");

    private final String description;

    CompensationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
