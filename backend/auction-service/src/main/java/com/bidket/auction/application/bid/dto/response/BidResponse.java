package com.bidket.auction.application.bid.dto.response;

import com.bidket.auction.domain.bid.model.Bid;
import com.bidket.auction.domain.bid.model.BidStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record BidResponse(
        UUID id,
        UUID auctionId,
        UUID bidderId,
        Long amount,
        boolean isHighest,
        BidStatus status,
        Integer rank,
        UUID orderId,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static BidResponse from(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getAuctionId(),
                bid.getBidderId(),
                bid.getAmount(),
                bid.isHighest(),
                bid.getStatus(),
                bid.getRank(),
                bid.getOrderId(),
                bid.getCreatedAt()
        );
    }
}


