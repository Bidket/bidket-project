package com.bidket.auction.domain.repository;

import com.bidket.auction.domain.model.Auction;
import com.bidket.auction.domain.model.AuctionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionRepository {

    Auction save(Auction auction);

    Optional<Auction> findById(UUID id);

    List<Auction> findBySellerId(UUID sellerId);

    List<Auction> findByStatus(AuctionStatus status);

    List<Auction> findActiveAuctionsEndingBefore(LocalDateTime dateTime);

    List<Auction> findPendingAuctionsStartingBefore(LocalDateTime dateTime);

    void delete(Auction auction);

    boolean existsById(UUID id);
}
