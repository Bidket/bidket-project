package com.bidket.auction.infrastructure.persistence.impl;

import com.bidket.auction.domain.model.Auction;
import com.bidket.auction.domain.model.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuctionJpaRepository extends JpaRepository<Auction, UUID> {

    List<Auction> findBySellerId(UUID sellerId);

    List<Auction> findByStatus(AuctionStatus status);

    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND a.period.endTime < :dateTime")
    List<Auction> findActiveAuctionsEndingBefore(@Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT a FROM Auction a WHERE a.status = 'PENDING' AND a.period.startTime < :dateTime")
    List<Auction> findPendingAuctionsStartingBefore(@Param("dateTime") LocalDateTime dateTime);

    @Modifying
    @Query("UPDATE Auction a SET a.stats.viewCount = :viewCount WHERE a.id = :auctionId")
    int updateViewCount(@Param("auctionId") UUID auctionId, @Param("viewCount") Integer viewCount);
}
