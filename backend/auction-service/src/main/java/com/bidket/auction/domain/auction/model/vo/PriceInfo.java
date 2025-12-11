package com.bidket.auction.domain.auction.model.vo;

import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import com.bidket.auction.global.exception.BidDomainException;
import com.bidket.auction.global.exception.BidErrorCode;
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
            throw new AuctionDomainException(AuctionErrorCode.INVALID_START_PRICE);
        }
        if (bidIncrement == null || bidIncrement <= 0) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_BID_INCREMENT);
        }
        if (buyNowPrice != null && buyNowPrice <= startPrice) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_BUY_NOW_PRICE);
        }
    }

    public PriceInfo withUpdatedCurrentPrice(Long newPrice) {
        if (newPrice <= this.currentPrice) {
            throw new BidDomainException(BidErrorCode.BID_AMOUNT_TOO_LOW);
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


