package com.bidket.auction.domain.auction.model;

import com.bidket.auction.domain.auction.model.vo.AuctionPeriod;
import com.bidket.auction.domain.auction.model.vo.AuctionStats;
import com.bidket.auction.domain.auction.model.vo.PriceInfo;
import com.bidket.auction.domain.auction.model.vo.WinnerInfo;
import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
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

/**
 * [Senior's Guide: 역할]
 * 이 클래스는 Bidket 경매 도메인의 핵심 Aggregate Root입니다.
 * 경매의 전체 생명주기를 관리하며, [경매 생성 → 확정 → 시작 → 입찰 → 종료 → 낙찰] 흐름의 중심에 있습니다.
 *
 * [도메인 흐름에서의 위치]
 * 1. 생성 단계: CREATING → PENDING (Product Service의 재고 확보 대기)
 * 2. 시작 단계: PENDING → ACTIVE (AuctionScheduler가 startTime 도래 시 자동 시작)
 * 3. 진행 단계: ACTIVE (사용자 입찰 진행, BidService와 협력)
 * 4. 종료 단계: ACTIVE → SUCCESS/EXPIRED (AuctionEndSaga 시작)
 * 5. 예외 흐름: SUCCESS → REOPENED (PaymentTimeoutSaga - 결제 타임아웃 시)
 *
 * [왜 이렇게 설계했을까?]
 *
 * 1. @NoArgsConstructor(access = AccessLevel.PROTECTED)
 *    - JPA는 엔티티 로드 시 기본 생성자가 필요합니다 (리플렉션 사용)
 *    - 하지만 외부에서 new Auction()으로 직접 생성하는 것은 불완전한 객체를 만들 수 있어 위험합니다
 *    - PROTECTED로 설정하여 JPA/프록시는 접근 가능하지만, 도메인 외부에서는 Builder만 사용하도록 강제했습니다
 *    - 이는 "항상 유효한 상태의 객체만 존재한다"는 도메인 불변식(Invariant)을 지키는 설계입니다
 *
 * 2. @Version (Optimistic Lock)
 *    - 여러 사용자가 동시에 같은 경매에 입찰할 때 데이터 일관성을 보장합니다
 *    - 예: User A와 User B가 동시에 입찰 → JPA가 version을 체크하여 하나만 성공
 *    - 실패한 입찰은 @RetryOnOptimisticLock AOP가 자동으로 재시도합니다
 *    - DB 락(Pessimistic Lock)보다 성능이 좋고, 동시성이 높은 경매 시스템에 적합합니다
 *
 * 3. @Embedded (Value Object 패턴)
 *    - PriceInfo, AuctionPeriod, WinnerInfo, AuctionStats는 개념적으로 Auction의 일부입니다
 *    - 별도 테이블로 분리하면 JOIN이 필요하고 복잡도가 증가합니다
 *    - Embedded로 설계하여 응집도를 높이고, 하나의 트랜잭션으로 원자적 업데이트를 보장합니다
 *    - "경매"라는 개념이 여러 속성의 집합이 아니라, 하나의 완전한 비즈니스 객체임을 명확히 합니다
 *
 * 4. @Index 설계 이유
 *    - idx_auction_status_end: 스케줄러가 "종료 시간 도래 & ACTIVE 상태" 경매를 빠르게 조회
 *    - idx_auction_seller: 판매자별 경매 목록 조회 (마이페이지)
 *    - idx_auction_product_size: 동일 상품에 대해 ACTIVE 경매가 이미 있는지 확인 (1개 신발 = 1개 경매 규칙)
 *    - idx_auction_winner: 낙찰자별 낙찰 내역 조회
 *
 * [어디서 이 클래스를 사용하나요?]
 * - {@link com.bidket.auction.application.auction.service.AuctionService}: 경매 생성, 조회, 수정
 * - {@link com.bidket.auction.application.bid.service.BidService}: 입찰 시 currentPrice 업데이트
 * - {@link com.bidket.auction.application.auction.scheduler.AuctionScheduler}: 경매 시작/종료
 * - {@link com.bidket.auction.application.saga.AuctionEndSagaOrchestrator}: 경매 종료 후 Saga 시작
 * - {@link com.bidket.auction.application.saga.PaymentTimeoutSagaOrchestrator}: 결제 타임아웃 시 경매 재오픈
 *
 * [신입 개발자 주의사항]
 * - 경매 상태 변경은 반드시 도메인 메서드를 사용하세요 (setStatus() 같은 setter는 없습니다)
 * - 예: auction.start(), auction.end(), auction.reopen()
 * - 직접 필드를 변경하면 비즈니스 규칙이 깨질 수 있습니다
 * - 모든 상태 전이는 validate()를 거치며, 잘못된 전이 시도는 예외가 발생합니다
 */
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
            throw new AuctionDomainException(AuctionErrorCode.INVALID_AUCTION_STATUS);
        }
        this.status = AuctionStatus.PENDING;
    }

    public void start() {
        if (this.status != AuctionStatus.PENDING) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_AUCTION_STATUS);
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
                throw new AuctionDomainException(AuctionErrorCode.CANNOT_CANCEL_WITH_BIDS);
            }
            this.status = AuctionStatus.CANCELLED;
            return;
        }

        throw new AuctionDomainException(AuctionErrorCode.INVALID_AUCTION_STATUS);
    }

    public void end(boolean hasBids) {
        if (this.status != AuctionStatus.ACTIVE) {
            throw new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_ACTIVE);
        }

        this.status = hasBids ? AuctionStatus.SUCCESS : AuctionStatus.EXPIRED;
    }

    public void extend() {
        if (this.status != AuctionStatus.ACTIVE) {
            throw new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_ACTIVE);
        }
        this.period = this.period.extend();
    }

    public void setWinner(UUID winnerId, UUID winningBidId, Long finalPrice) {
        if (this.status != AuctionStatus.SUCCESS) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_AUCTION_STATUS);
        }
        this.winnerInfo = WinnerInfo.of(winnerId, winningBidId, finalPrice);
    }

    public void reopen() {
        if (this.status != AuctionStatus.SUCCESS) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_AUCTION_STATUS);
        }

        this.winnerInfo = WinnerInfo.empty();
        this.period = this.period.withReopenedEndTime(LocalDateTime.now().plusDays(1));
        this.status = AuctionStatus.ACTIVE;
    }

    public void updateCurrentPrice(Long newPrice) {
        if (this.status != AuctionStatus.ACTIVE) {
            throw new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_ACTIVE);
        }
        this.priceInfo = this.priceInfo.withUpdatedCurrentPrice(newPrice);
        this.stats = this.stats.incrementBidCount();
    }

    public void update(String auctionTitle, String description,
                      LocalDateTime startTime, LocalDateTime endTime, Long buyNowPrice) {
        if (this.status != AuctionStatus.PENDING) {
            throw new AuctionDomainException(AuctionErrorCode.INVALID_AUCTION_STATUS);
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


