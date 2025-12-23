package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 블랙리스트 해제 응답 DTO
 */
@Builder
public record BlacklistReleaseResponse(
        /* 대상 회원 ID */
        UUID memberId,
        
        /* 블랙리스트 여부 (false) */
        Boolean blacklisted,
        
        /* 블랙리스트 해제 일시 */
        LocalDateTime updatedAt
) {
}

