package com.bidket.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 알림 삭제 응답 DTO
 */
@Builder
@Schema(description = "알림 삭제 응답")
public record DeleteNotificationResponse(
        @Schema(description = "삭제 성공 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean success,

        @Schema(description = "삭제 결과 메시지", example = "알림이 삭제되었습니다.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String message
) {
}

