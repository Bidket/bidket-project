package com.bidket.auction.application.auction.dto.request;

import com.bidket.auction.domain.auction.model.Auction;
import com.bidket.auction.domain.auction.model.AuctionCondition;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAuctionRequest(
        @NotNull(message = "상품 사이즈 ID는 필수입니다")
        UUID productSizeId,

        @NotNull(message = "판매자 ID는 필수입니다")
        UUID sellerId,

        @NotBlank(message = "경매 제목은 필수입니다")
        @Size(min = 5, max = 200, message = "경매 제목은 5자 이상 200자 이하여야 합니다")
        String auctionTitle,

        @Size(max = 2000, message = "설명은 최대 2000자까지 입력 가능합니다")
        String description,

        @NotNull(message = "상품 상태는 필수입니다")
        AuctionCondition condition,

        @NotNull(message = "시작가는 필수입니다")
        @Min(value = 10000, message = "시작가는 최소 10,000원 이상이어야 합니다")
        Long startPrice,

        @Min(value = 1000, message = "입찰 단위는 최소 1,000원 이상이어야 합니다")
        Long bidIncrement,

        Long buyNowPrice,

        @NotNull(message = "시작 시간은 필수입니다")
        @Future(message = "시작 시간은 현재 시간 이후여야 합니다")
        LocalDateTime startTime,

        @NotNull(message = "종료 시간은 필수입니다")
        LocalDateTime endTime
) {
    public Auction toEntity() {
        return Auction.builder()
                .productSizeId(productSizeId)
                .sellerId(sellerId)
                .auctionTitle(auctionTitle)
                .description(description)
                .condition(condition)
                .startPrice(startPrice)
                .bidIncrement(bidIncrement)
                .buyNowPrice(buyNowPrice)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }
}


