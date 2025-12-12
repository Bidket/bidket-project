package com.bidket.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 블랙리스트 등록 요청 DTO
 */
@Builder
@Schema(description = "블랙리스트 등록 요청")
public record BlacklistRegisterRequest(
        /** 블랙리스트 등록 사유 */
        @NotBlank(message = "블랙리스트 등록 사유는 필수입니다.")
        @Schema(description = "블랙리스트 등록 사유", example = "가품판매 시도 / 적발")
        String reason,
        
        /** 블랙리스트 해제 예정일 (없으면 무기한) */
        @Schema(description = "블랙리스트 해제 예정일 (없으면 무기한)", example = "2025-03-31T23:59:59")
        LocalDateTime expireAt
) {
}

