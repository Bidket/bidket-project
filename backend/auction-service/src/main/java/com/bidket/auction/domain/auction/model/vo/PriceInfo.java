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
public class PriceInfo {

    @Column(nullable = false, name = "start_price")
    private Long startPrice;

    @Column(nullable = false, name = "current_price")
    private Long currentPrice;

    @Column(nullable = false, name = "bid_increment")
    private Long bidIncrement;

    @Column(name = "buy_now_price")
    private Long buyNowPrice;

    public static PriceInfo createDefault(Long startPrice, Long bidIncrement, Long buyNowPrice) {
        return PriceInfo.builder()
                .startPrice(startPrice)
                .currentPrice(startPrice)
                .bidIncrement(bidIncrement != null ? bidIncrement : 10000L)
                .buyNowPrice(buyNowPrice)
                .build();
    }

    public void validate() {
        if (startPrice == null || startPrice <= 0) {
            throw new IllegalArgumentException("시작가는 0보다 커야 합니다");
        }
        if (bidIncrement == null || bidIncrement <= 0) {
            throw new IllegalArgumentException("입찰 단위는 0보다 커야 합니다");
        }
        if (buyNowPrice != null && buyNowPrice <= startPrice) {
            throw new IllegalArgumentException("즉시구매가는 시작가보다 커야 합니다");
        }
    }

    public PriceInfo withUpdatedCurrentPrice(Long newPrice) {
        if (newPrice <= this.currentPrice) {
            throw new IllegalArgumentException("새 가격은 현재가보다 커야 합니다");
        }
        return PriceInfo.builder()
                .startPrice(this.startPrice)
                .currentPrice(newPrice)
                .bidIncrement(this.bidIncrement)
                .buyNowPrice(this.buyNowPrice)
                .build();
    }

    public PriceInfo withBuyNowPrice(Long buyNowPrice) {
        return PriceInfo.builder()
                .startPrice(this.startPrice)
                .currentPrice(this.currentPrice)
                .bidIncrement(this.bidIncrement)
                .buyNowPrice(buyNowPrice)
                .build();
    }
}


