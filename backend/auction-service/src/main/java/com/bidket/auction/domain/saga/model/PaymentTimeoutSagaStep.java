package com.bidket.auction.domain.saga.model;

public enum PaymentTimeoutSagaStep {
     
    REOPEN_AUCTION("경매 재오픈"),

    REVERT_BID_STATUS("입찰 상태 복원"),

    CANCEL_ORDER("주문 취소"),

    PUBLISH_REOPEN_EVENT("재오픈 이벤트 발행");

    private final String description;

    PaymentTimeoutSagaStep(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public PaymentTimeoutSagaStep next() {
        return switch (this) {
            case REOPEN_AUCTION -> REVERT_BID_STATUS;
            case REVERT_BID_STATUS -> CANCEL_ORDER;
            case CANCEL_ORDER -> PUBLISH_REOPEN_EVENT;
            case PUBLISH_REOPEN_EVENT -> null;
        };
    }
}
