package com.bidket.auction.domain.bid.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BidAmount {

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, name = "is_highest")
    private boolean highest;

    @Column(name = "rank")
    private Integer rank;

    public Long getAmount() {
        return amount;
    }

    public boolean isHighest() {
        return highest;
    }

    public Integer getRank() {
        return rank;
    }

    public static BidAmount of(Long amount) {
        return BidAmount.builder()
                .amount(amount)
                .highest(false)
                .rank(null)
                .build();
    }

    public BidAmount markAsHighest() {
        return BidAmount.builder()
                .amount(this.amount)
                .highest(true)
                .rank(this.rank)
                .build();
    }

    public BidAmount markAsOutbid() {
        return BidAmount.builder()
                .amount(this.amount)
                .highest(false)
                .rank(this.rank)
                .build();
    }

    public BidAmount withRank(Integer rank) {
        return BidAmount.builder()
                .amount(this.amount)
                .highest(this.highest)
                .rank(rank)
                .build();
    }
}


