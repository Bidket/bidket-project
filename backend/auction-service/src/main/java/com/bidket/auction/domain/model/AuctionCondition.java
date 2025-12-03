package com.bidket.auction.domain.model;

public enum AuctionCondition {
    NEW("새 제품"),
    USED("중고"),
    DEADSTOCK("미사용");

    private final String description;

    AuctionCondition(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
