package com.bidket.auction.domain.auction.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WinnerInfo {

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "winning_bid_id")
    private UUID winningBidId;

    @Column(name = "final_price")
    private Long finalPrice;

    public static WinnerInfo empty() {
        return new WinnerInfo(null, null, null);
    }

    public static WinnerInfo of(UUID winnerId, UUID winningBidId, Long finalPrice) {
        return WinnerInfo.builder()
                .winnerId(winnerId)
                .winningBidId(winningBidId)
                .finalPrice(finalPrice)
                .build();
    }

    public boolean hasWinner() {
        return winnerId != null;
    }
}


