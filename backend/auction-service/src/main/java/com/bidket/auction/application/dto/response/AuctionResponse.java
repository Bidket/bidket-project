package com.bidket.auction.application.dto.response;

import com.bidket.auction.domain.model.Auction;
import com.bidket.auction.domain.model.AuctionCondition;
import com.bidket.auction.domain.model.AuctionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuctionResponse(
        UUID id,
        UUID productSizeId,
        UUID sellerId,
        String auctionTitle,
        String description,
        AuctionCondition condition,
        Long startPrice,
        Long currentPrice,
        Long bidIncrement,
        Long buyNowPrice,
        AuctionStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime originalEndTime,
        Integer extensionCount,
        UUID winnerId,
        UUID winningBidId,
        Long finalPrice,
        Integer totalBidsCount,
        Integer viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AuctionResponse from(Auction auction) {
        return new AuctionResponse(
                auction.getId(),
                auction.getProductSizeId(),
                auction.getSellerId(),
                auction.getAuctionTitle(),
                auction.getDescription(),
                auction.getCondition(),
                auction.getStartPrice(),
                auction.getCurrentPrice(),
                auction.getBidIncrement(),
                auction.getBuyNowPrice(),
                auction.getStatus(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getOriginalEndTime(),
                auction.getExtensionCount(),
                auction.getWinnerId(),
                auction.getWinningBidId(),
                auction.getFinalPrice(),
                auction.getTotalBidsCount(),
                auction.getViewCount(),
                auction.getCreatedAt(),
                auction.getUpdatedAt()
        );
    }

    public static AuctionResponse fromSummary(Auction auction) {
        return new AuctionResponse(
                auction.getId(),
                auction.getProductSizeId(),
                auction.getSellerId(),
                auction.getAuctionTitle(),
                null,
                auction.getCondition(),
                auction.getStartPrice(),
                auction.getCurrentPrice(),
                auction.getBidIncrement(),
                auction.getBuyNowPrice(),
                auction.getStatus(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getOriginalEndTime(),
                auction.getExtensionCount(),
                null,
                null,
                auction.getFinalPrice(),
                auction.getTotalBidsCount(),
                auction.getViewCount(),
                auction.getCreatedAt(),
                auction.getUpdatedAt()
        );
    }
}
