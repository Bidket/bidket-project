package com.bidket.auction.domain.saga.model;

public enum SagaStep {
     
    CREATE_ORDER("주문 생성 요청"),

    MARK_WINNING_BID("낙찰 입찰 표시"),

    FINALIZE_AUCTION("경매 종료 확정"),

    PUBLISH_END_EVENT("종료 이벤트 발행");

    private final String description;

    SagaStep(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public SagaStep next() {
        return switch (this) {
            case CREATE_ORDER -> MARK_WINNING_BID;
            case MARK_WINNING_BID -> FINALIZE_AUCTION;
            case FINALIZE_AUCTION -> PUBLISH_END_EVENT;
            case PUBLISH_END_EVENT -> null;
        };
    }
}
