package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 블랙리스트 등록 응답 DTO
 */
@Builder
public record BlacklistRegisterResponse(
        /** 대상 회원 ID */
        UUID memberId,
        
        /** 블랙리스트 여부 (true) */
        Boolean blacklisted,
        
        /** 등록 사유 */
        String reason,
        
        /** 해제 예정일 (무기한인 경우 null) */
        LocalDateTime expireAt,
        
        /** 블랙리스트 등록 일시 */
        LocalDateTime createdAt
) {
}

