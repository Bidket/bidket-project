package com.bidket.auction.domain.saga.model;

/**
 * 결제 타임아웃 Saga의 단계
 * BACKLOG.md SAGA-002 참조
 *
 * Flow:
 * 1. REOPEN_AUCTION: 경매 상태를 REOPENED로 변경하고 endTime을 +24h 연장
 * 2. REVERT_BID_STATUS: 이전 낙찰 입찰을 WON에서 ACTIVE로 되돌림
 * 3. CANCEL_ORDER: Order Service에 주문 취소 요청
 * 4. PUBLISH_REOPEN_EVENT: AUCTION_REOPENED 이벤트 발행
 *
 * Note: 재고 복원 단계 제거됨
 * - Bidket은 크림/StockX와 같은 리셀 플랫폼
 * - 1개 신발 = 1개 경매 (재고 개념 없음)
 * - 결제 타임아웃 시 경매 재오픈으로 처리
 */
public enum PaymentTimeoutSagaStep {
    /**
     * Step 1: 경매 재오픈
     */
    REOPEN_AUCTION("경매 재오픈"),

    /**
     * Step 2: 입찰 상태 복원
     */
    REVERT_BID_STATUS("입찰 상태 복원"),

    /**
     * Step 3: 주문 취소 요청
     */
    CANCEL_ORDER("주문 취소"),

    /**
     * Step 4: AUCTION_REOPENED 이벤트 발행
     */
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
