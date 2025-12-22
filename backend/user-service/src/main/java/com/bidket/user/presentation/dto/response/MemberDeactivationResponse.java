package com.bidket.user.presentation.dto.response;

import com.bidket.user.domain.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 회원 탈퇴(비활성화) 응답 DTO
 */
@Builder
@Schema(description = "회원 탈퇴(비활성화) 응답")
public record MemberDeactivationResponse(
        /** 비활성화된 회원 ID */
        @Schema(description = "비활성화된 회원 ID", example = "c12f92cd-7a6e-4c92-ae01-e77ca1cafe56")
        UUID memberId,

        /** 변경된 상태 */
        @Schema(description = "변경된 상태", example = "WITHDRAWN")
        UserStatus status,

        /** 비활성화 처리 일시 */
        @Schema(description = "비활성화 처리 일시", example = "2025-11-27T13:00:00", type = "string", format = "date-time")
        LocalDateTime deactivatedAt
) {
}

