package com.bidket.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 회원 탈퇴(비활성화) 요청 DTO
 */
@Schema(description = "회원 탈퇴(비활성화) 요청")
public record MemberDeactivationRequest(
        /** 탈퇴(비활성화) 사유 (설문/통계용) */
        @Schema(description = "탈퇴(비활성화) 사유 (설문/통계용)", example = "서비스를 더 이상 사용하지 않음")
        String reason
) {
}

