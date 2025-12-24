package com.bidket.user.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 내역 항목 응답 DTO
 */
@Builder
public record OrderHistoryItemResponse(
        /** 주문 ID */
        UUID orderId,
        
        /** 주문 상태값 */
        String status,
        
        /** 주문금액 */
        Long amount,
        
        /** 상품명 */
        String productName,
        
        /** 경매 제목 */
        String auctionTitle,
        
        /** 경매 시작 시간 */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime auctionStartTime,
        
        /** 생성 시각 */
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
}

