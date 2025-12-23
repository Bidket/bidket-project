package com.bidket.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 재발급 요청 DTO
 */
@Schema(description = "토큰 재발급 요청")
public record TokenRefreshRequest(
        /** 기존에 발급된 리프레시 토큰 */
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        @Schema(description = "기존에 발급된 리프레시 토큰", requiredMode = Schema.RequiredMode.REQUIRED, example = "refresh.jwt.token")
        String refreshToken
) {
}

