package com.bidket.notification.presentation.controller;

import com.bidket.notification.application.service.NotificationService;
// import com.bidket.notification.global.security.AuthenticationHelper;
import com.bidket.notification.presentation.dto.request.SendNotificationRequest;
import com.bidket.notification.presentation.dto.response.SendNotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 컨트롤러
 */
@Tag(name = "알림", description = "알림 관련 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 단건 발송 API
     * 특정 사용자에게 Slack 알림을 바로 보내는 API
     * 관리자·내부 시스템에서 직접 발송하거나 Kafka 이벤트 처리 후 내부 호출로도 사용
     *
     * @param request 알림 발송 요청
     * @return 알림 발송 응답
     */
    @Operation(
            summary = "알림 단건 발송",
            description = "Slack 알림을 바로 보내는 API. ROLE_ADMIN 권한 필요.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "알림 발송 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "권한 없음 (ROLE_ADMIN 필요)"
                    )
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/send")
    public ResponseEntity<SendNotificationResponse> sendNotification(
            @Valid @RequestBody SendNotificationRequest request
    ) {
        // ROLE_ADMIN 권한 확인
        // AuthenticationHelper.requireAdminRole();
        
        SendNotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
