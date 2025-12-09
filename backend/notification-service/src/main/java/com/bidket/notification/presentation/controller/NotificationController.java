package com.bidket.notification.presentation.controller;

import com.bidket.notification.application.service.NotificationService;
import com.bidket.notification.global.security.AuthenticationHelper;
import com.bidket.notification.presentation.dto.request.SendNotificationRequest;
import com.bidket.notification.presentation.dto.response.InAppNotificationListResponse;
import com.bidket.notification.presentation.dto.response.SendNotificationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 알림 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 인앱 알림 목록 조회 API
     *
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @param onlyUnread 읽지 않은 알림만 조회 (true인 경우 읽지 않은 알림만 조회)
     * @param category 알림 카테고리 (BID_SUCCESS, AUCTION_START 등)
     * @return 인앱 알림 목록 응답
     */
    @GetMapping("/in-app")
    public ResponseEntity<InAppNotificationListResponse> getInAppNotifications(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Boolean onlyUnread,
            @RequestParam(required = false) String category
    ) {
        UUID userId = AuthenticationHelper.getCurrentUserId();

        InAppNotificationListResponse response = notificationService.getInAppNotifications(
                userId,
                page,
                size,
                onlyUnread,
                category
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 알림 단건 발송 API
     * 특정 사용자에게 알림을 바로 보내는 API
     * - 관리자 콘솔에서 직접 알림 발송 또는 내부 배치 프로세스에서 사용
     * - 모든 사용자 사용 가능 (권한 체크 주석처리)
     *
     * @param request 알림 발송 요청
     * @return 알림 발송 응답
     */
    @PostMapping("/send")
    public ResponseEntity<SendNotificationResponse> sendNotification(
            @Valid @RequestBody SendNotificationRequest request
    ) {
        // ROLE_ADMIN 권한 체크 (주석처리 - 모든 사용자 사용 가능, 추후변경예정)
        // AuthenticationHelper.requireAdminRole();

        SendNotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.ok(response);
    }
}

