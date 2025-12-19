package com.bidket.auction.domain.bid.model;

import com.bidket.auction.domain.bid.model.vo.BidAmount;
import com.bidket.auction.domain.bid.model.vo.BidMetadata;
import com.bidket.auction.domain.bid.model.vo.BidResult;
import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.bidket.auction.global.exception.BidDomainException;
import com.bidket.auction.global.exception.BidErrorCode;
import java.util.UUID;

/**
 * [Senior's Guide: 역할]
 * 이 클래스는 사용자의 '입찰' 행위를 표현하는 도메인 엔티티입니다.
 * 경매 흐름 중 [입찰 등록 → 최고가 경쟁 → 낙찰/패찰] 단계를 담당합니다.
 *
 * [도메인 흐름에서의 위치]
 * 1. 입찰 생성: PENDING (초기 상태, 아직 최고가 아님)
 * 2. 최고가 설정: ACTIVE (isHighest=true, 현재 최고가 입찰)
 * 3. 밀림: OUTBID (더 높은 입찰이 들어와 최고가 상실)
 * 4. 낙찰: WON (경매 종료 시 최고가 → 낙찰자)
 * 5. 취소: CANCELLED (OUTBID 상태에서만 취소 가능)
 * 6. 복원: WON → ACTIVE (결제 타임아웃 시 보상 트랜잭션)
 *
 * [왜 이렇게 설계했을까?]
 *
 * 1. @NoArgsConstructor(access = AccessLevel.PROTECTED)
 *    - Auction 클래스와 동일한 이유: JPA 요구사항 충족 + 외부 직접 생성 방지
 *    - Builder 패턴을 통해서만 생성하도록 강제하여 "금액이 0원인 입찰" 같은 불완전한 객체를 방지합니다
 *
 * 2. @Version (Optimistic Lock)
 *    - 동일 경매에 여러 입찰이 동시에 발생할 때, isHighest 플래그의 일관성을 보장합니다
 *    - 예: User A와 User B가 동시에 입찰 → 한 명만 isHighest=true를 가져갑니다
 *    - Auction 엔티티의 version과는 독립적으로 관리되어 입찰 자체의 동시성도 제어합니다
 *
 * 3. @Embedded (Value Object 패턴)
 *    - BidAmount: 금액, isHighest, rank 같은 "입찰 금액 정보"를 하나의 개념으로 묶음
 *    - BidResult: orderId 같은 "입찰 결과 정보"
 *    - BidMetadata: idempotencyKey 같은 "입찰 메타데이터"
 *    - 이렇게 분리하면 관심사가 명확해지고, "입찰 금액만 변경" 같은 연산이 안전해집니다
 *
 * 4. @Index 설계 이유
 *    - idx_bid_auction_created: 경매별 입찰 내역 조회 (최신순 정렬)
 *    - idx_bid_bidder: 사용자별 입찰 내역 조회 (마이페이지)
 *    - idx_bid_auction_highest: 경매의 최고가 입찰 빠르게 찾기 (SELECT * WHERE auction_id=? AND is_highest=true)
 *    - idx_bid_status: 상태별 입찰 조회 (예: 취소된 입찰만 모아보기)
 *
 * [입찰 상태 전이 규칙]
 * - PENDING → ACTIVE: markAsHighest() - 최고가 입찰로 설정
 * - ACTIVE → OUTBID: markAsOutbid() - 더 높은 입찰에 밀림
 * - ACTIVE → WON: markAsWon() - 경매 종료 시 낙찰
 * - OUTBID → CANCELLED: cancel() - 사용자가 입찰 취소
 * - WON → ACTIVE: revertToActive() - 결제 타임아웃 보상 (PaymentTimeoutSaga)
 *
 * [어디서 이 클래스를 사용하나요?]
 * - {@link com.bidket.auction.application.bid.service.BidService}: 입찰 등록, 취소, 조회
 * - {@link com.bidket.auction.application.saga.AuctionEndSagaOrchestrator}: 낙찰 입찰 WON 상태로 변경
 * - {@link com.bidket.auction.application.saga.PaymentTimeoutSagaOrchestrator}: 결제 타임아웃 시 입찰 복원
 * - {@link com.bidket.auction.application.compensation.actions.RevertBidStatusCompensationAction}: 보상 트랜잭션
 *
 * [신입 개발자 주의사항]
 * - 최고가 입찰(isHighest=true)은 절대 취소할 수 없습니다 (cancel() 호출 시 예외 발생)
 * - 입찰 상태 변경은 반드시 도메인 메서드를 사용하세요 (setter 없음)
 * - "입찰 금액"은 BidAmount VO에 있으므로, getAmount()로 접근합니다
 * - isHighest 플래그는 경매당 정확히 1개만 true여야 합니다 (BidService가 보장)
 */
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
            throw new BidDomainException(BidErrorCode.INVALID_BID_AMOUNT);
        }
        if (this.auctionId == null) {
            throw new BidDomainException(BidErrorCode.BID_CONFLICT);
        }
        if (this.bidderId == null) {
            throw new BidDomainException(BidErrorCode.BID_CONFLICT);
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
            throw new BidDomainException(BidErrorCode.BID_CONFLICT);
        }
        this.status = BidStatus.WON;
    }

    public void revertFromWon() {
        if (this.status != BidStatus.WON) {
            throw new BidDomainException(BidErrorCode.BID_CONFLICT);
        }
        this.status = BidStatus.ACTIVE;
        this.bidAmount = this.bidAmount.markAsHighest();
    }

    public void cancel() {
        if (this.isHighest()) {
            throw new BidDomainException(BidErrorCode.CANNOT_CANCEL_HIGHEST_BID);
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


