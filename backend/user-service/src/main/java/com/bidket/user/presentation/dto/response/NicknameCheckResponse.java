package com.bidket.user.presentation.dto.response;

import lombok.Builder;

/**
 * 닉네임 중복 체크 응답 DTO
 */
@Builder
public record NicknameCheckResponse(
        /** 확인한 닉네임 */
        String nickname,
        
        /** 사용 가능한 경우 true, 이미 사용 중인 경우 false */
        Boolean available,
        
        /** 사용 불가인 경우 사유 코드 (ALREADY_USED, INVALID_FORMAT) */
        String reason
) {
}

