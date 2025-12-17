package com.bidket.notification.presentation.controller;

import com.bidket.notification.application.service.NotificationService;
import com.bidket.notification.presentation.dto.request.SendNotificationRequest;
import com.bidket.notification.presentation.dto.response.DeleteNotificationResponse;
import com.bidket.notification.presentation.dto.response.ReadNotificationResponse;
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

import java.util.UUID;

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
     * 특정 사용자에게 알림을 바로 보내는 API (SLACK, EMAIL, IN_APP 지원)
     * 관리자·내부 시스템에서 직접 발송하거나 Kafka 이벤트 처리 후 내부 호출로도 사용
     * @param request 알림 발송 요청
     * @return 알림 발송 응답
     */
    @Operation(
            summary = "알림 단건 발송",
            description = "알림을 바로 보내는 API (SLACK, EMAIL, IN_APP 지원). ROLE_ADMIN 권한 필요.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "알림 발송 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "403", description = "권한 없음 (ROLE_ADMIN 필요)")
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/send")
    public ResponseEntity<SendNotificationResponse> sendNotification(
            @Valid @RequestBody SendNotificationRequest request
    ) {
        SendNotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 인앱 알림 읽음 처리 API
     * In-App 알림만 사용 가능하며, 본인 소유 알림에 대해서만 읽음 처리가 허용됩니다.
     * @param notificationId 읽음 처리할 인앱 알림 ID
     * @return 읽음 처리 응답
     */
    @Operation(
            summary = "인앱 알림 읽음 처리",
            description = "In-App 알림을 읽음 처리하는 API. ROLE_USER 이상 권한 필요. 본인 소유 알림에 대해서만 처리 가능.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "403", description = "권한 없음 (본인 소유 알림이 아님)"),
                    @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음"),
                    @ApiResponse(responseCode = "400", description = "In-App 알림이 아님")
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ReadNotificationResponse> markNotificationAsRead(
            @PathVariable UUID notificationId
    ) {
        ReadNotificationResponse response = notificationService.markNotificationAsRead(notificationId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 인앱 알림 삭제 API
     * In-App 알림만 사용 가능하며, 본인 소유 알림에 대해서만 삭제가 허용됩니다.
     * @param notificationId 삭제할 인앱 알림 ID
     * @return 삭제 처리 응답
     */
    @Operation(
            summary = "인앱 알림 삭제",
            description = "In-App 알림을 삭제하는 API (soft delete). ROLE_USER 이상 권한 필요. 본인 소유 알림에 대해서만 삭제 가능.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "삭제 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "403", description = "권한 없음 (본인 소유 알림이 아님)"),
                    @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음 또는 이미 삭제된 알림"),
                    @ApiResponse(responseCode = "400", description = "In-App 알림이 아님")
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<DeleteNotificationResponse> deleteNotification(
            @PathVariable UUID notificationId
    ) {
        DeleteNotificationResponse response = notificationService.deleteNotification(notificationId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
