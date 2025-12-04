package com.bidket.user.presentation.dto.response;

import lombok.Builder;

/**
 * 에러 응답 DTO
 */
@Builder
public record ErrorResponse(
        Boolean success,
        String errorCode,
        String message,
        Integer status,
        Object data
) {
}

