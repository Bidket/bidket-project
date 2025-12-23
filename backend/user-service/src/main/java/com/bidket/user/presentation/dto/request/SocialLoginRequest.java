package com.bidket.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 소셜 로그인 요청 DTO
 */
@Schema(description = "소셜 로그인 요청")
public record SocialLoginRequest(
        /** 소셜 타입 (GOOGLE) */
        @NotBlank(message = "provider는 필수입니다.")
        @Schema(description = "소셜 타입", example = "GOOGLE", requiredMode = Schema.RequiredMode.REQUIRED)
        String provider,
        
        /** GOOGLE 로그인 시 필수: idToken */
        @Schema(description = "Google idToken (Google 로그인 후 발급받은 토큰)", example = "", requiredMode = Schema.RequiredMode.REQUIRED)
        String idToken,
        
        /** 디바이스 고유 ID (선택) */
        @Schema(description = "디바이스 고유 ID", example = "test-device-123")
        String deviceId,
        
        /** 마케팅 동의 여부 (선택, 최초 자동가입 시에만 저장) */
        @Schema(description = "마케팅 알림 수신 동의", example = "true")
        Boolean marketingAgree
) {
}

