package com.bidket.user.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 블랙리스트 등록 요청 DTO
 */
@Builder
public record BlacklistRegisterRequest(
        /** 블랙리스트 등록 사유 */
        @NotBlank(message = "블랙리스트 등록 사유는 필수입니다.")
        String reason,
        
        /** 블랙리스트 해제 예정일 (없으면 무기한) */
        LocalDateTime expireAt
) {
}

