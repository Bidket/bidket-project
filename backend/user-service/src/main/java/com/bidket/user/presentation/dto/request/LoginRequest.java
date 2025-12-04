package com.bidket.user.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * 로그인 요청 DTO
 * 
 * LOCAL 방식 로그인: loginId + password로 로그인
 */
@Builder
public record LoginRequest(
        /** 로그인 아이디 (p_user.login_id) */
        @NotBlank(message = "로그인 아이디는 필수입니다.")
        String loginId,

        /** 비밀번호 (서버에서 해시 비교) */
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}

