package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 입찰 가능 여부 체크 응답 DTO
 */
@Builder
public record BidEligibilityResponse(
        /** 입찰 가능 여부 */
        Boolean eligible,
        
        /** 불가능한 경우 사유 코드 리스트 (예: BLACKLISTED, INACTIVE_MEMBER 등) */
        List<String> reasons,
        
        /** 회원 상태 (ACTIVE, SUSPENDED, WITHDRAWN 등) */
        String memberStatus
) {
}

