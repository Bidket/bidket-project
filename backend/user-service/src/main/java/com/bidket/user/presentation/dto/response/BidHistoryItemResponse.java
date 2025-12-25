package com.bidket.user.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 입찰 내역 항목 응답 DTO
 */
@Builder
public record BidHistoryItemResponse(
        /** 입찰 ID */
        UUID id,
        
        /** 경매 ID */
        UUID auctionId,
        
        /** 입찰자 ID */
        UUID bidderId,
        
        /** 입찰금액 */
        Long amount,
        
        /** 입찰 중 최고가 여부 */
        Boolean highest,
        
        /** 입찰 상태값 */
        String status,
        
        /** 입찰 순위 */
        Integer rank,
        
        /** 낙찰 후 주문 ID (있을 때만) */
        UUID orderId,
        
        /** 생성 시각 */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
}

