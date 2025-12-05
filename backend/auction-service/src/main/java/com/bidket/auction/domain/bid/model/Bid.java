package com.bidket.auction.domain.bid.model;

import com.bidket.auction.domain.bid.model.vo.BidAmount;
import com.bidket.auction.domain.bid.model.vo.BidMetadata;
import com.bidket.auction.domain.bid.model.vo.BidResult;
import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "bid", indexes = {
    @Index(name = "idx_bid_auction_created", columnList = "auction_id, created_at"),
    @Index(name = "idx_bid_bidder", columnList = "bidder_id, created_at"),
    @Index(name = "idx_bid_auction_highest", columnList = "auction_id, is_highest"),
    @Index(name = "idx_bid_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bid extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "auction_id")
    private UUID auctionId;

    @Column(nullable = false, name = "bidder_id")
    private UUID bidderId;

    @Embedded
    private BidAmount bidAmount;

    @Embedded
    private BidResult bidResult;

    @Embedded
    private BidMetadata bidMetadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BidStatus status;

    @Version
    private Long version;

    // ===== Getter 메서드 =====

    public Long getAmount() {
        return bidAmount != null ? bidAmount.getAmount() : null;
    }

    public boolean isHighest() {
        return bidAmount != null ? bidAmount.isHighest() : false;
    }

    public Integer getRank() {
        return bidAmount != null ? bidAmount.getRank() : null;
    }

    public UUID getOrderId() {
        return bidResult != null ? bidResult.getOrderId() : null;
    }

    public String getIdempotencyKey() {
        return bidMetadata != null ? bidMetadata.getIdempotencyKey() : null;
    }

    // Private 생성자
    private Bid(UUID id, UUID auctionId, UUID bidderId, BidAmount bidAmount,
                BidResult bidResult, BidMetadata bidMetadata, BidStatus status, Long version) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidResult = bidResult != null ? bidResult : BidResult.empty();
        this.bidMetadata = bidMetadata != null ? bidMetadata : BidMetadata.empty();
        this.status = status;
        this.version = version;
    }

    // Builder
    public static BidBuilder builder() {
        return new BidBuilder();
    }

    public static class BidBuilder {
        private UUID id;
        private UUID auctionId;
        private UUID bidderId;
        private Long amount;
        private boolean isHighest = false;
        private Integer rank;
        private UUID orderId;
        private String idempotencyKey;
        private BidStatus status;
        private Long version;

        // 하위 호환성을 위한 레거시 메서드들
        public BidBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public BidBuilder auctionId(UUID auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public BidBuilder bidderId(UUID bidderId) {
            this.bidderId = bidderId;
            return this;
        }

        public BidBuilder amount(Long amount) {
            this.amount = amount;
            return this;
        }

        public BidBuilder isHighest(boolean isHighest) {
            this.isHighest = isHighest;
            return this;
        }

        public BidBuilder rank(Integer rank) {
            this.rank = rank;
            return this;
        }

        public BidBuilder orderId(UUID orderId) {
            this.orderId = orderId;
            return this;
        }

        public BidBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public BidBuilder status(BidStatus status) {
            this.status = status;
            return this;
        }

        public BidBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public Bid build() {
            // VO 생성
            BidAmount bidAmount = BidAmount.builder()
                    .amount(this.amount)
                    .highest(this.isHighest)
                    .rank(this.rank)
                    .build();

            BidResult bidResult = this.orderId != null ?
                    BidResult.builder().orderId(this.orderId).build() :
                    BidResult.empty();

            BidMetadata bidMetadata = this.idempotencyKey != null ?
                    BidMetadata.builder().idempotencyKey(this.idempotencyKey).build() :
                    BidMetadata.empty();

            // 기본값 설정
            BidStatus resolvedStatus = this.status != null ? this.status : BidStatus.PENDING;

            Bid bid = new Bid(
                this.id,
                this.auctionId,
                this.bidderId,
                bidAmount,
                bidResult,
                bidMetadata,
                resolvedStatus,
                this.version
            );

            bid.validate();
            return bid;
        }
    }

    private void validate() {
        if (this.bidAmount == null || this.bidAmount.getAmount() == null || this.bidAmount.getAmount() <= 0) {
            throw new IllegalArgumentException("입찰 금액은 0보다 커야 합니다");
        }
        if (this.auctionId == null) {
            throw new IllegalArgumentException("경매 ID는 필수입니다");
        }
        if (this.bidderId == null) {
            throw new IllegalArgumentException("입찰자 ID는 필수입니다");
        }
    }

    // ===== 도메인 비즈니스 메서드 =====

    public void markAsHighest() {
        this.bidAmount = this.bidAmount.markAsHighest();
        this.status = BidStatus.ACTIVE;
    }

    public void markAsOutbid() {
        this.bidAmount = this.bidAmount.markAsOutbid();
        this.status = BidStatus.OUTBID;
    }

    public void markAsWon() {
        if (this.status != BidStatus.ACTIVE) {
            throw new IllegalStateException("ACTIVE 상태에서만 낙찰될 수 있습니다");
        }
        this.status = BidStatus.WON;
    }

    public void cancel() {
        if (this.isHighest()) {
            throw new IllegalStateException("최고가 입찰은 취소할 수 없습니다");
        }
        this.status = BidStatus.CANCELLED;
    }

    public void reject() {
        this.status = BidStatus.REJECTED;
    }

    public void setOrderId(UUID orderId) {
        this.bidResult = this.bidResult.withOrderId(orderId);
    }

    public void setRank(Integer rank) {
        this.bidAmount = this.bidAmount.withRank(rank);
    }
}


