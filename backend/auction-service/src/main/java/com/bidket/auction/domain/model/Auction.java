package com.bidket.auction.domain.model;

import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auction", indexes = {
    @Index(name = "idx_auction_status_end", columnList = "status, end_time"),
    @Index(name = "idx_auction_seller", columnList = "seller_id"),
    @Index(name = "idx_auction_product_size", columnList = "product_size_id"),
    @Index(name = "idx_auction_winner", columnList = "winner_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Auction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "product_size_id")
    private UUID productSizeId;

    @Column(nullable = false, name = "seller_id")
    private UUID sellerId;

    @Column(nullable = false, length = 200, name = "auction_title")
    private String auctionTitle;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionCondition condition;

    @Column(nullable = false, name = "start_price")
    private Long startPrice;

    @Column(nullable = false, name = "current_price")
    private Long currentPrice;

    @Column(nullable = false, name = "bid_increment")
    private Long bidIncrement;

    @Column(name = "buy_now_price")
    private Long buyNowPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @Column(nullable = false, name = "start_time")
    private LocalDateTime startTime;

    @Column(nullable = false, name = "end_time")
    private LocalDateTime endTime;

    @Column(nullable = false, name = "original_end_time")
    private LocalDateTime originalEndTime;

    @Column(nullable = false, name = "extension_count")
    private Integer extensionCount;

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "winning_bid_id")
    private UUID winningBidId;

    @Column(name = "final_price")
    private Long finalPrice;

    @Column(nullable = false, name = "total_bids_count")
    private Integer totalBidsCount;

    @Column(nullable = false, name = "view_count")
    private Integer viewCount;

    @Version
    private Long version;

    public static AuctionBuilder builder() {
        return new AuctionBuilderImpl();
    }

    private static class AuctionBuilderImpl extends AuctionBuilder {
        @Override
        public Auction build() {
            if (super.currentPrice == null && super.startPrice != null) {
                super.currentPrice = super.startPrice;
            }
            if (super.originalEndTime == null && super.endTime != null) {
                super.originalEndTime = super.endTime;
            }
            if (super.bidIncrement == null) {
                super.bidIncrement = 10000L;
            }
            if (super.status == null) {
                super.status = AuctionStatus.CREATING;
            }
            if (super.extensionCount == null) {
                super.extensionCount = 0;
            }
            if (super.totalBidsCount == null) {
                super.totalBidsCount = 0;
            }
            if (super.viewCount == null) {
                super.viewCount = 0;
            }

            Auction auction = super.build();
            auction.validate();
            return auction;
        }
    }

    private void validate() {
        validatePrice();
        validateTimeRange();
    }

    private void validatePrice() {
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

    private void validateTimeRange() {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("시작시간과 종료시간은 필수입니다");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("종료시간은 시작시간보다 이후여야 합니다");
        }
    }

    public void confirmCreation() {
        if (this.status != AuctionStatus.CREATING) {
            throw new IllegalStateException("CREATING 상태에서만 생성을 확정할 수 있습니다");
        }
        this.status = AuctionStatus.PENDING;
    }

    public void start() {
        if (this.status != AuctionStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 시작할 수 있습니다");
        }
        this.status = AuctionStatus.ACTIVE;
    }

    public void cancel() {
        if (this.status == AuctionStatus.PENDING) {
            this.status = AuctionStatus.CANCELLED;
            return;
        }

        if (this.status == AuctionStatus.ACTIVE) {
            if (this.totalBidsCount > 0) {
                throw new IllegalStateException("입찰이 있는 경매는 취소할 수 없습니다");
            }
            this.status = AuctionStatus.CANCELLED;
            return;
        }

        throw new IllegalStateException("취소할 수 없는 상태입니다: " + this.status);
    }

    public void end(boolean hasBids) {
        if (this.status != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("ACTIVE 상태에서만 종료할 수 있습니다");
        }

        if (hasBids) {
            this.status = AuctionStatus.SUCCESS;
        } else {
            this.status = AuctionStatus.EXPIRED;
        }
    }

    public void extend() {
        if (this.status != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("ACTIVE 상태에서만 연장할 수 있습니다");
        }

        final int MAX_EXTENSIONS = 3;
        if (this.extensionCount >= MAX_EXTENSIONS) {
            throw new IllegalStateException("최대 연장 횟수를 초과했습니다");
        }

        final int EXTENSION_MINUTES = 5;
        this.endTime = this.endTime.plusMinutes(EXTENSION_MINUTES);
        this.extensionCount++;
    }

    public void setWinner(UUID winnerId, UUID winningBidId, Long finalPrice) {
        if (this.status != AuctionStatus.SUCCESS) {
            throw new IllegalStateException("SUCCESS 상태에서만 낙찰자를 설정할 수 있습니다");
        }
        this.winnerId = winnerId;
        this.winningBidId = winningBidId;
        this.finalPrice = finalPrice;
    }

    public void reopen() {
        if (this.status != AuctionStatus.SUCCESS) {
            throw new IllegalStateException("SUCCESS 상태에서만 재오픈할 수 있습니다");
        }

        this.status = AuctionStatus.REOPENED;
        this.winnerId = null;
        this.winningBidId = null;
        this.finalPrice = null;
        this.endTime = LocalDateTime.now().plusDays(1);
        this.status = AuctionStatus.ACTIVE;
    }

    public void updateCurrentPrice(Long newPrice) {
        if (this.status != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("ACTIVE 상태에서만 가격을 업데이트할 수 있습니다");
        }
        if (newPrice <= this.currentPrice) {
            throw new IllegalArgumentException("새 가격은 현재가보다 커야 합니다");
        }
        this.currentPrice = newPrice;
        this.totalBidsCount++;
    }

    @Deprecated
    public void incrementViewCount() {
        this.viewCount++;
    }

    public void update(String auctionTitle, String description, 
                      LocalDateTime startTime, LocalDateTime endTime, Long buyNowPrice) {
        if (this.status != AuctionStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 수정할 수 있습니다");
        }

        if (auctionTitle != null) {
            this.auctionTitle = auctionTitle;
        }
        if (description != null) {
            this.description = description;
        }
        if (startTime != null) {
            this.startTime = startTime;
            this.originalEndTime = endTime != null ? endTime : this.endTime;
        }
        if (endTime != null) {
            this.endTime = endTime;
        }
        if (buyNowPrice != null) {
            this.buyNowPrice = buyNowPrice;
        }

        validate();
    }

    public boolean isNearEnd() {
        return LocalDateTime.now().isAfter(this.endTime.minusMinutes(5));
    }

    public boolean isEnded() {
        return LocalDateTime.now().isAfter(this.endTime);
    }
}
