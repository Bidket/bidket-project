package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 포인트 잔액 조회 응답 DTO
 */
@Builder
public record PointBalanceResponse(
        /** 회원 ID (UUID) */
        UUID memberId,
        
        /** 현재 사용 가능한 포인트 잔액 */
        Long balance,
        
        /** 포인트 기준 통화/단위 (예: POINT) */
        String currency,
        
        /** 잔액 기준 시각 */
        LocalDateTime updatedAt
) {
}

