package com.bidket.notification.presentation.dto.request;

import lombok.Builder;

/**
 * 알림 수신 설정 업데이트 요청 DTO
 */
@Builder
public record UpdateNotificationSettingRequest(
        Boolean allowPush,
        Boolean allowEmail,
        Boolean allowSms,
        Boolean allowMarketing
) {
}

