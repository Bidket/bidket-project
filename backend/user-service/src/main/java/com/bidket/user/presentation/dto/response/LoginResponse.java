package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.util.UUID;

/**
 * 로그인 응답 DTO
 */
@Builder
public record LoginResponse(
        /** JWT 액세스 토큰 */
        String accessToken,
        
        /** JWT 리프레시 토큰 */
        String refreshToken,
        
        /** 토큰 타입 (일반적으로 Bearer) */
        String tokenType,
        
        /** 액세스 토큰 만료까지 남은 시간(초 단위) */
        Long expiresIn,
        
        /** 회원 ID (UUID) */
        UUID memberId,
        
        /** 로그인 아이디 */
        String loginId,
        
        /** 닉네임 */
        String nickname,
        
        /** 이름 */
        String name,
        
        /** 이메일 (username) */
        String email,
        
        /** 권한 */
        String role,
        
        /** 회원 상태 (ACTIVE, SUSPENDED, WITHDRAWN) */
        String status
) {
}

