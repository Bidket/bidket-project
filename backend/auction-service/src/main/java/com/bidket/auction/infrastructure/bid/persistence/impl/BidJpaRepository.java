package com.bidket.auction.infrastructure.bid.persistence.impl;

import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.model.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidJpaRepository extends JpaRepository<Bid, UUID> {

    List<Bid> findByAuctionIdOrderByCreatedAtDesc(UUID auctionId);

    List<Bid> findByBidderIdOrderByCreatedAtDesc(UUID bidderId);

    // 최고가 입찰 조회: Embedded VO 경로 + 메서드명 기반 쿼리
    Optional<Bid> findFirstByAuctionIdAndBidAmount_HighestTrue(UUID auctionId);

    List<Bid> findByAuctionIdAndBidderId(UUID auctionId, UUID bidderId);

    List<Bid> findByStatus(BidStatus status);

    boolean existsByAuctionIdAndBidderId(UUID auctionId, UUID bidderId);

    // 멱등성 키로 조회: Embedded VO 경로 사용
    Optional<Bid> findByBidMetadata_IdempotencyKey(String idempotencyKey);

    long countByAuctionId(UUID auctionId);
}


