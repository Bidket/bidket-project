package com.bidket.auction.domain.saga.model;

/**
 * 경매 종료 Saga의 단계
 * BACKLOG.md line 712-757 참조
 */
public enum SagaStep {
    /**
     * Step 1: Order Service에 주문 생성 요청
     */
    CREATE_ORDER("주문 생성 요청"),

    /**
     * Step 2: 낙찰 입찰 상태 업데이트 (WON)
     */
    MARK_WINNING_BID("낙찰 입찰 표시"),

    /**
     * Step 3: 경매 최종 상태 업데이트 (SUCCESS)
     */
    FINALIZE_AUCTION("경매 종료 확정"),

    /**
     * Step 4: AUCTION_ENDED 이벤트 발행
     */
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
