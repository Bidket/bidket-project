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
                auction.getPriceInfo().getStartPrice(),
                auction.getPriceInfo().getCurrentPrice(),
                auction.getPriceInfo().getBidIncrement(),
                auction.getPriceInfo().getBuyNowPrice(),
                auction.getStatus(),
                auction.getPeriod().getStartTime(),
                auction.getPeriod().getEndTime(),
                auction.getPeriod().getOriginalEndTime(),
                auction.getPeriod().getExtensionCount(),
                auction.getWinnerInfo().getWinnerId(),
                auction.getWinnerInfo().getWinningBidId(),
                auction.getWinnerInfo().getFinalPrice(),
                auction.getStats().getTotalBidsCount(),
                auction.getStats().getViewCount(),
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
                auction.getPriceInfo().getStartPrice(),
                auction.getPriceInfo().getCurrentPrice(),
                auction.getPriceInfo().getBidIncrement(),
                auction.getPriceInfo().getBuyNowPrice(),
                auction.getStatus(),
                auction.getPeriod().getStartTime(),
                auction.getPeriod().getEndTime(),
                auction.getPeriod().getOriginalEndTime(),
                auction.getPeriod().getExtensionCount(),
                null,
                null,
                auction.getWinnerInfo().getFinalPrice(),
                auction.getStats().getTotalBidsCount(),
                auction.getStats().getViewCount(),
                auction.getCreatedAt(),
                auction.getUpdatedAt()
        );
    }

    public AuctionResponse withViewCount(Integer viewCount) {
        return new AuctionResponse(
                this.id,
                this.productSizeId,
                this.sellerId,
                this.auctionTitle,
                this.description,
                this.condition,
                this.startPrice,
                this.currentPrice,
                this.bidIncrement,
                this.buyNowPrice,
                this.status,
                this.startTime,
                this.endTime,
                this.originalEndTime,
                this.extensionCount,
                this.winnerId,
                this.winningBidId,
                this.finalPrice,
                this.totalBidsCount,
                viewCount,
                this.createdAt,
                this.updatedAt
        );
    }
}
