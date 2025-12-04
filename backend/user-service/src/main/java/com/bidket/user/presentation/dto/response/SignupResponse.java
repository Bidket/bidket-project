package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 회원가입 응답 DTO
 */
@Builder
public record SignupResponse(
        /** 생성된 회원 ID (p_user.id) */
        UUID memberId,
        
        /** 로그인 아이디 */
        String loginId,
        
        /** 이메일 */
        String email,
        
        /** 닉네임 */
        String nickname,
        
        /** 가입 일시 (created_at) - ISO-8601 형식 */
        LocalDateTime createdAt,
        
        /** JWT 액세스 토큰 */
        String accessToken,
        
        /** JWT 리프레시 토큰 */
        String refreshToken
) {
}

