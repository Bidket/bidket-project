package com.bidket.auction.domain.model;

import com.bidket.auction.domain.model.vo.AuctionPeriod;
import com.bidket.auction.domain.model.vo.AuctionStats;
import com.bidket.auction.domain.model.vo.PriceInfo;
import com.bidket.auction.domain.model.vo.WinnerInfo;
import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

    @Embedded
    private PriceInfo priceInfo;

    @Embedded
    private AuctionPeriod period;

    @Embedded
    private WinnerInfo winnerInfo;

    @Embedded
    private AuctionStats stats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @Version
    private Long version;

    // ===== Private 생성자 =====
    private Auction(UUID id, UUID productSizeId, UUID sellerId, String auctionTitle, 
                    String description, AuctionCondition condition, PriceInfo priceInfo,
                    AuctionPeriod period, WinnerInfo winnerInfo, AuctionStats stats,
                    AuctionStatus status, Long version) {
        this.id = id;
        this.productSizeId = productSizeId;
        this.sellerId = sellerId;
        this.auctionTitle = auctionTitle;
        this.description = description;
        this.condition = condition;
        this.priceInfo = priceInfo;
        this.period = period;
        this.winnerInfo = winnerInfo;
        this.stats = stats;
        this.status = status;
        this.version = version;
    }

    // ===== Builder =====
    public static AuctionBuilder builder() {
        return new AuctionBuilder();
    }

    public static class AuctionBuilder {
        private UUID id;
        private UUID productSizeId;
        private UUID sellerId;
        private String auctionTitle;
        private String description;
        private AuctionCondition condition;
        private PriceInfo priceInfo;
        private AuctionPeriod period;
        private WinnerInfo winnerInfo;
        private AuctionStats stats;
        private AuctionStatus status;
        private Long version;

        // 하위 호환성을 위한 개별 필드 (레거시 코드 지원용)
        private Long startPrice;
        private Long currentPrice;
        private Long bidIncrement;
        private Long buyNowPrice;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime originalEndTime;
        private Integer extensionCount;
        private UUID winnerId;
        private UUID winningBidId;
        private Long finalPrice;
        private Integer totalBidsCount;
        private Integer viewCount;

        public AuctionBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public AuctionBuilder productSizeId(UUID productSizeId) {
            this.productSizeId = productSizeId;
            return this;
        }

        public AuctionBuilder sellerId(UUID sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public AuctionBuilder auctionTitle(String auctionTitle) {
            this.auctionTitle = auctionTitle;
            return this;
        }

        public AuctionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public AuctionBuilder condition(AuctionCondition condition) {
            this.condition = condition;
            return this;
        }

        public AuctionBuilder priceInfo(PriceInfo priceInfo) {
            this.priceInfo = priceInfo;
            return this;
        }

        public AuctionBuilder period(AuctionPeriod period) {
            this.period = period;
            return this;
        }

        public AuctionBuilder winnerInfo(WinnerInfo winnerInfo) {
            this.winnerInfo = winnerInfo;
            return this;
        }

        public AuctionBuilder stats(AuctionStats stats) {
            this.stats = stats;
            return this;
        }

        public AuctionBuilder status(AuctionStatus status) {
            this.status = status;
            return this;
        }

        public AuctionBuilder version(Long version) {
            this.version = version;
            return this;
        }

        // ===== 하위 호환성 메서드 (레거시) =====
        public AuctionBuilder startPrice(Long startPrice) {
            this.startPrice = startPrice;
            return this;
        }

        public AuctionBuilder currentPrice(Long currentPrice) {
            this.currentPrice = currentPrice;
            return this;
        }

        public AuctionBuilder bidIncrement(Long bidIncrement) {
            this.bidIncrement = bidIncrement;
            return this;
        }

        public AuctionBuilder buyNowPrice(Long buyNowPrice) {
            this.buyNowPrice = buyNowPrice;
            return this;
        }

        public AuctionBuilder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public AuctionBuilder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public AuctionBuilder originalEndTime(LocalDateTime originalEndTime) {
            this.originalEndTime = originalEndTime;
            return this;
        }

        public AuctionBuilder extensionCount(Integer extensionCount) {
            this.extensionCount = extensionCount;
            return this;
        }

        public AuctionBuilder winnerId(UUID winnerId) {
            this.winnerId = winnerId;
            return this;
        }

        public AuctionBuilder winningBidId(UUID winningBidId) {
            this.winningBidId = winningBidId;
            return this;
        }

        public AuctionBuilder finalPrice(Long finalPrice) {
            this.finalPrice = finalPrice;
            return this;
        }

        public AuctionBuilder totalBidsCount(Integer totalBidsCount) {
            this.totalBidsCount = totalBidsCount;
            return this;
        }

        public AuctionBuilder viewCount(Integer viewCount) {
            this.viewCount = viewCount;
            return this;
        }

        public Auction build() {
            // PriceInfo 생성
            PriceInfo resolvedPriceInfo = this.priceInfo;
            if (resolvedPriceInfo == null && this.startPrice != null) {
                resolvedPriceInfo = PriceInfo.builder()
                        .startPrice(this.startPrice)
                        .currentPrice(this.currentPrice != null ? this.currentPrice : this.startPrice)
                        .bidIncrement(this.bidIncrement != null ? this.bidIncrement : 10000L)
                        .buyNowPrice(this.buyNowPrice)
                        .build();
            }

            // AuctionPeriod 생성
            AuctionPeriod resolvedPeriod = this.period;
            if (resolvedPeriod == null && this.startTime != null) {
                resolvedPeriod = AuctionPeriod.builder()
                        .startTime(this.startTime)
                        .endTime(this.endTime)
                        .originalEndTime(this.originalEndTime != null ? this.originalEndTime : this.endTime)
                        .extensionCount(this.extensionCount != null ? this.extensionCount : 0)
                        .build();
            }

            // WinnerInfo 생성
            WinnerInfo resolvedWinnerInfo = this.winnerInfo;
            if (resolvedWinnerInfo == null) {
                if (this.winnerId != null || this.winningBidId != null || this.finalPrice != null) {
                    resolvedWinnerInfo = WinnerInfo.builder()
                            .winnerId(this.winnerId)
                            .winningBidId(this.winningBidId)
                            .finalPrice(this.finalPrice)
                            .build();
                } else {
                    resolvedWinnerInfo = WinnerInfo.empty();
                }
            }

            // AuctionStats 생성
            AuctionStats resolvedStats = this.stats;
            if (resolvedStats == null) {
                resolvedStats = AuctionStats.builder()
                        .totalBidsCount(this.totalBidsCount != null ? this.totalBidsCount : 0)
                        .viewCount(this.viewCount != null ? this.viewCount : 0)
                        .build();
            }

            // 기본값 설정
            AuctionStatus resolvedStatus = this.status != null ? this.status : AuctionStatus.CREATING;

            Auction auction = new Auction(
                    this.id,
                    this.productSizeId,
                    this.sellerId,
                    this.auctionTitle,
                    this.description,
                    this.condition,
                    resolvedPriceInfo,
                    resolvedPeriod,
                    resolvedWinnerInfo,
                    resolvedStats,
                    resolvedStatus,
                    this.version
            );

            auction.validate();
            return auction;
        }
    }

    private void validate() {
        if (priceInfo != null) {
            priceInfo.validate();
        }
        if (period != null) {
            period.validate();
        }
    }

    // ===== 도메인 비즈니스 메서드 =====
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
            if (this.stats.getTotalBidsCount() > 0) {
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

        this.status = hasBids ? AuctionStatus.SUCCESS : AuctionStatus.EXPIRED;
    }

    public void extend() {
        if (this.status != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("ACTIVE 상태에서만 연장할 수 있습니다");
        }
        this.period = this.period.extend();
    }

    public void setWinner(UUID winnerId, UUID winningBidId, Long finalPrice) {
        if (this.status != AuctionStatus.SUCCESS) {
            throw new IllegalStateException("SUCCESS 상태에서만 낙찰자를 설정할 수 있습니다");
        }
        this.winnerInfo = WinnerInfo.of(winnerId, winningBidId, finalPrice);
    }

    public void reopen() {
        if (this.status != AuctionStatus.SUCCESS) {
            throw new IllegalStateException("SUCCESS 상태에서만 재오픈할 수 있습니다");
        }

        this.winnerInfo = WinnerInfo.empty();
        this.period = this.period.withReopenedEndTime(LocalDateTime.now().plusDays(1));
        this.status = AuctionStatus.ACTIVE;
    }

    public void updateCurrentPrice(Long newPrice) {
        if (this.status != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("ACTIVE 상태에서만 가격을 업데이트할 수 있습니다");
        }
        this.priceInfo = this.priceInfo.withUpdatedCurrentPrice(newPrice);
        this.stats = this.stats.incrementBidCount();
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
        if (startTime != null || endTime != null) {
            this.period = this.period.withUpdatedTimes(startTime, endTime);
        }
        if (buyNowPrice != null) {
            this.priceInfo = this.priceInfo.withBuyNowPrice(buyNowPrice);
        }

        validate();
    }

    public boolean isNearEnd() {
        return period != null && period.isNearEnd();
    }

    public boolean isEnded() {
        return period != null && period.isEnded();
    }
}

