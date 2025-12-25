package com.bidket.auction.infrastructure.bid.persistence.impl;

import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.model.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidJpaRepository extends JpaRepository<Bid, UUID> {

    List<Bid> findByAuctionIdOrderByCreatedAtDesc(UUID auctionId);

    List<Bid> findByBidderIdOrderByCreatedAtDesc(UUID bidderId);

    Optional<Bid> findFirstByAuctionIdAndBidAmount_HighestTrue(UUID auctionId);

    /**
     * 최고 입찰을 조회하는 최적화된 쿼리
     * - idx_bid_auction_highest 인덱스 사용
     * - is_highest = true 조건으로 빠른 조회
     * - 캐싱과 함께 사용하여 성능 극대화
     */
    @Query(value = """
        SELECT b.* FROM bid b
        WHERE b.auction_id = :auctionId
        AND b.is_highest = true
        LIMIT 1
        """, nativeQuery = true)
    Optional<Bid> findHighestBidOptimized(@Param("auctionId") UUID auctionId);

    List<Bid> findByAuctionIdAndBidderId(UUID auctionId, UUID bidderId);

    List<Bid> findByStatus(BidStatus status);

    boolean existsByAuctionIdAndBidderId(UUID auctionId, UUID bidderId);

    Optional<Bid> findByBidMetadata_IdempotencyKey(String idempotencyKey);

    long countByAuctionId(UUID auctionId);
}
