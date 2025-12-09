package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * 권한 조회 응답 DTO
 */
@Builder
public record PermissionsResponse(
        /** 회원 ID (UUID) */
        UUID memberId,
        
        /** 보유 롤 목록 (ROLE_USER, ROLE_ADMIN 등) */
        List<String> roles
) {
}

