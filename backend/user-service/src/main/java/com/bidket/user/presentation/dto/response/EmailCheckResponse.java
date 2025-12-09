package com.bidket.user.presentation.dto.response;

import lombok.Builder;

/**
 * 이메일 중복 체크 응답 DTO
 */
@Builder
public record EmailCheckResponse(
        /** 확인한 이메일 주소 */
        String email,
        
        /** 사용 가능한 경우 true, 이미 사용 중인 경우 false */
        Boolean available,
        
        /** 사용 불가인 경우 사유 코드 (ALREADY_USED, INVALID_FORMAT) */
        String reason
) {
}

