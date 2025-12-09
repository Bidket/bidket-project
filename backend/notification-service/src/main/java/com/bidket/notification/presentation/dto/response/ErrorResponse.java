package com.bidket.notification.presentation.dto.response;

import lombok.Builder;

/**
 * 에러 응답 DTO
 */
@Builder
public record ErrorResponse(
        boolean success,
        String errorCode,
        String message,
        int status,
        Object data
) {
}

