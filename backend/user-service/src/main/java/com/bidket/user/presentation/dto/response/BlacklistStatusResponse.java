package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 블랙리스트 상태 조회 응답 DTO
 */
@Builder
public record BlacklistStatusResponse(
        /** 회원 ID (UUID) */
        UUID memberId,
        
        /** 블랙리스트 여부 (true: 블랙리스트, false: 정상) */
        Boolean isBlacklisted,
        
        /** 블랙리스트 사유 (블랙리스트가 아닌 경우 null) */
        String reason,
        
        /** 블랙리스트 만료 시간 (무기한인 경우 null) */
        LocalDateTime expireAt
) {
}

