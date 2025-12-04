package com.bidket.auction.infrastructure.auction.persistence.impl;

import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionStatus;
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

    List<Auction> findByStatusAndPeriod_EndTimeBefore(AuctionStatus status, LocalDateTime dateTime);

    List<Auction> findByStatusAndPeriod_StartTimeBefore(AuctionStatus status, LocalDateTime dateTime);

    @Modifying
    @Query(
            value = "UPDATE auction SET view_count = :viewCount WHERE id = :auctionId",
            nativeQuery = true
    )
    int updateViewCount(@Param("auctionId") UUID auctionId, @Param("viewCount") Integer viewCount);
}


