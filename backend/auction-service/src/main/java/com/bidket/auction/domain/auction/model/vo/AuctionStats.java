package com.bidket.auction.domain.auction.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuctionStats {

    @Column(nullable = false, name = "total_bids_count")
    private Integer totalBidsCount;

    @Column(nullable = false, name = "view_count")
    private Integer viewCount;

    public static AuctionStats createDefault() {
        return AuctionStats.builder()
                .totalBidsCount(0)
                .viewCount(0)
                .build();
    }

    public AuctionStats incrementBidCount() {
        return AuctionStats.builder()
                .totalBidsCount(this.totalBidsCount + 1)
                .viewCount(this.viewCount)
                .build();
    }
}


