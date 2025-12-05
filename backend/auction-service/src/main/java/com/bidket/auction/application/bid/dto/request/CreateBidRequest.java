package com.bidket.auction.application.bid.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateBidRequest(
        @NotNull(message = "경매 ID는 필수입니다")
        UUID auctionId,

        @NotNull(message = "입찰 금액은 필수입니다")
        @Min(value = 1000, message = "입찰 금액은 최소 1,000원 이상이어야 합니다")
        Long amount
) {
}


