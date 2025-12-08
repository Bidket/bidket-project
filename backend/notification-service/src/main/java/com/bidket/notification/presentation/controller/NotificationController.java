package com.bidket.notification.presentation.controller;

import com.bidket.notification.application.service.NotificationService;
import com.bidket.notification.global.security.AuthenticationHelper;
import com.bidket.notification.presentation.dto.response.InAppNotificationListResponse;
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
}

