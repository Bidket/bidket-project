package com.bidket.user.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 토큰 재발급 응답 DTO
 */
@Schema(description = "토큰 재발급 응답")
public record TokenRefreshResponse(
        /** 새로 발급된 액세스 토큰 */
        @Schema(description = "새로 발급된 액세스 토큰", example = "new.access.jwt.token")
        String accessToken,

        /** 재발급된 리프레시 토큰 */
        @Schema(description = "재발급된 리프레시 토큰. 재발급 성공 시 Refresh Token은 회전되며, 기존 RT는 즉시 무효화된다.", example = "new.refresh.jwt.token")
        String refreshToken,

        /** 토큰 타입 */
        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        /** 새 액세스 토큰 만료까지 남은 시간(초) */
        @Schema(description = "새 액세스 토큰 만료까지 남은 시간(초)", example = "3600")
        Long expiresIn
) {
}

