package com.bidket.user.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * 회원가입 요청 DTO
 * 
 * LOCAL 방식 회원가입: loginId + password로 로그인
 * email(username)은 연락용/계정 식별용
 */
@Builder
public record SignupRequest(
        /** 사용자가 지정하는 로그인 아이디 (p_user.login_id) */
        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Size(max = 30, message = "로그인 아이디는 30자 이하여야 합니다.")
        String loginId,

        /** 비밀번호 (BCrypt 해시 저장, p_user.password) */
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        String password,

        /** 이메일 (p_user.username) */
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        /** 닉네임 (p_user.nickname) - 선택 */
        String nickname,

        /** 이름 (p_user.name) */
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
        String name,

        /** 전화번호 (p_user.phone) */
        @NotBlank(message = "전화번호는 필수입니다.")
        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        String phone,

        /** 마케팅 알림 수신 동의 여부 (알림 설정 테이블에 반영) */
        @NotNull(message = "마케팅 알림 수신 동의 여부는 필수입니다.")
        Boolean marketingAgree,

        /** 이용 약관 동의 여부 (DB 컬럼은 없고 서버에서 검증만 수행) */
        @NotNull(message = "이용 약관 동의 여부는 필수입니다.")
        Boolean termsAgreement
) {
}

