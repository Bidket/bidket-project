package com.bidket.user.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 프로필 수정 응답 DTO
 */
@Builder
@Schema(description = "프로필 수정 응답")
public record ProfileUpdateResponse(
        /** 회원 ID (UUID) */
        @Schema(description = "회원 ID", example = "c12f92cd-7a6e-4c92-ae01-e77ca1cafe56")
        UUID userId,

        /** 변경 후 닉네임 */
        @Schema(description = "변경 후 닉네임", example = "콘서트대장")
        String nickname,

        /** 변경 후 전화번호 (미변경/미보유면 null 가능) */
        @Schema(description = "변경 후 전화번호", example = "01012345678", nullable = true)
        String phone,

        /** 변경 후 이메일 (미변경/미보유면 null 가능) */
        @Schema(description = "변경 후 이메일", example = "newmail@example.com", nullable = true)
        String email,

        /** 수정 시각 */
        @Schema(description = "수정 시각", example = "2025-11-27T12:10:00", type = "string", format = "date-time")
        LocalDateTime updatedAt
) {
}

