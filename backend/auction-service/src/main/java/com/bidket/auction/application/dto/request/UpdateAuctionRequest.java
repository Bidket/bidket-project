package com.bidket.auction.application.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateAuctionRequest(
        @Size(min = 5, max = 200, message = "경매 제목은 5자 이상 200자 이하여야 합니다")
        String auctionTitle,

        @Size(max = 2000, message = "설명은 최대 2000자까지 입력 가능합니다")
        String description,

        @Future(message = "시작 시간은 현재 시간 이후여야 합니다")
        LocalDateTime startTime,

        LocalDateTime endTime,

        Long buyNowPrice
) {
}
