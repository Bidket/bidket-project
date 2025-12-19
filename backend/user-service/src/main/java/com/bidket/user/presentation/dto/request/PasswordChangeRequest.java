package com.bidket.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 변경 요청 DTO
 */
@Schema(description = "비밀번호 변경 요청")
public record PasswordChangeRequest(
        /** 기존 비밀번호 */
        @NotBlank(message = "기존 비밀번호는 필수입니다.")
        @Schema(description = "기존 비밀번호", requiredMode = Schema.RequiredMode.REQUIRED)
        String currentPassword,

        /** 새 비밀번호 */
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Schema(description = "새 비밀번호", requiredMode = Schema.RequiredMode.REQUIRED)
        String newPassword
) {
}

