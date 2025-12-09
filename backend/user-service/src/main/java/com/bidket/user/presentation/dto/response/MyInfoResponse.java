package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 내 정보 조회 응답 DTO
 */
@Builder
public record MyInfoResponse(
        /** 회원 ID (UUID) */
        UUID memberId,
        
        /** 로그인 아이디 */
        String loginId,
        
        /** 이메일 */
        String email,
        
        /** 닉네임 */
        String nickname,
        
        /** 권한 (ROLE_USER, ROLE_ADMIN 등) */
        String role,
        
        /** 가입일 */
        LocalDateTime createdAt,
        
        /** 회원 상태 (ACTIVE, SUSPENDED, WITHDRAWN) */
        String status
) {
}

