package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 포인트 히스토리 항목 응답 DTO
 */
@Builder
public record PointHistoryItemResponse(
        /** 포인트 히스토리 ID */
        Long historyId,
        
        /** 거래 유형 (CHARGE, USE, REFUND, CANCEL 등) */
        String type,
        
        /** 변동 포인트 (양수: 적립, 음수: 사용) */
        Long amount,
        
        /** 해당 거래 후 잔액 */
        Long balanceAfter,
        
        /** 상세 설명 */
        String description,
        
        /** 관련 경매 ID (있다면) */
        Long relatedAuctionId,
        
        /** 관련 주문 ID (있다면) */
        UUID relatedOrderId,
        
        /** 거래 발생 일시 */
        LocalDateTime createdAt
) {
}

