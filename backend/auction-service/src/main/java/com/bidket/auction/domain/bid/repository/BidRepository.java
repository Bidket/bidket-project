package com.bidket.auction.domain.bid.repository;

import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.model.BidStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidRepository {

    Bid save(Bid bid);

    Optional<Bid> findById(UUID id);

    List<Bid> findByAuctionId(UUID auctionId);

    List<Bid> findByBidderId(UUID bidderId);

    Optional<Bid> findHighestBidByAuctionId(UUID auctionId);

    List<Bid> findByAuctionIdAndBidderId(UUID auctionId, UUID bidderId);

    List<Bid> findByStatus(BidStatus status);

    boolean existsByAuctionIdAndBidderId(UUID auctionId, UUID bidderId);

    Optional<Bid> findByIdempotencyKey(String idempotencyKey);

    void delete(Bid bid);

    long countByAuctionId(UUID auctionId);
}


