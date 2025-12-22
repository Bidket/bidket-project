package com.bidket.user.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 비밀번호 변경 응답 DTO
 */
@Builder
@Schema(description = "비밀번호 변경 응답")
public record PasswordChangeResponse(
        /** 비밀번호 변경 성공 여부 */
        @Schema(description = "비밀번호 변경 성공 여부", example = "true")
        boolean success,

        /** 비밀번호 변경 시각 */
        @Schema(description = "비밀번호 변경 시각", example = "2025-11-27T12:10:00", type = "string", format = "date-time")
        LocalDateTime changedAt
) {
}

