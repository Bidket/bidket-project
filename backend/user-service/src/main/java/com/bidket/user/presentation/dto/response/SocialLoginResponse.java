package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.util.UUID;

/**
 * 소셜 로그인 응답 DTO
 */
@Builder
public record SocialLoginResponse(
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
        
        /** 이메일 (소셜에서 제공된 값, 없을 수 있음) */
        String email,
        
        /** 이름 (소셜에서 제공된 값, 없을 수 있음) */
        String name,
        
        /** 닉네임 (없을 수 있음, 최초 NULL 가능) */
        String nickname,
        
        /** 소셜 타입 (GOOGLE) */
        String provider,
        
        /** 회원 상태 (ACTIVE, SUSPENDED, WITHDRAWN) */
        String status,
        
        /** 이번 요청으로 신규 가입이 발생했는지 여부 */
        Boolean isNewMember
) {
}

