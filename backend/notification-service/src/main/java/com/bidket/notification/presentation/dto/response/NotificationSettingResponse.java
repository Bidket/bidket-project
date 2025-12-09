package com.bidket.notification.presentation.dto.response;

import lombok.Builder;

import java.util.UUID;

/**
 * 알림 수신 설정 응답 DTO
 */
@Builder
public record NotificationSettingResponse(
        UUID userId,
        boolean allowPush,
        boolean allowEmail,
        boolean allowSms,
        boolean allowMarketing
) {
}

