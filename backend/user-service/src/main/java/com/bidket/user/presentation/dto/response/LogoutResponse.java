package com.bidket.user.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그아웃 응답 DTO
 */
@Schema(description = "로그아웃 응답")
public record LogoutResponse(
        /** 로그아웃 처리 성공 여부 */
        @Schema(description = "로그아웃 처리 성공 여부", example = "true")
        boolean success
) {
}

