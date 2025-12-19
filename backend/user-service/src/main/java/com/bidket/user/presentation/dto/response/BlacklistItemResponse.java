package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 블랙리스트 목록 항목 응답 DTO
 */
@Builder
public record BlacklistItemResponse(
        /* 회원 ID */
        UUID memberId,
        
        /* 닉네임 */
        String nickname,
        
        /* 블랙리스트 사유 */
        String reason,
        
        /* 해제 예정일 (무기한인 경우 null) */
        LocalDateTime expireAt,
        
        /* 등록 일시 */
        LocalDateTime createdAt,
        
        /* 현재 블랙리스트 유효 여부 */
        Boolean active
) {
}

