package com.bidket.notification.application.service;

import com.bidket.notification.infrastructure.persistence.entity.Notification;
import com.bidket.notification.infrastructure.persistence.repository.NotificationRepository;
import com.bidket.notification.presentation.dto.response.InAppNotificationListResponse;
import com.bidket.notification.presentation.dto.response.InAppNotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 알림 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    /**
     * 인앱 알림 목록 조회
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param onlyUnread 읽지 않은 알림만 조회 여부
     * @param category 알림 카테고리 (BID_SUCCESS, AUCTION_START 등)
     * @return 인앱 알림 목록 응답
     */
    public InAppNotificationListResponse getInAppNotifications(
            UUID userId,
            Integer page,
            Integer size,
            Boolean onlyUnread,
            String category
    ) {
        // 기본값 설정
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        // 인앱 알림 조회 (SYSTEM 채널만)
        Page<Notification> notificationPage = notificationRepository.findInAppNotificationsByUserId(
                userId,
                onlyUnread,
                category,
                pageable
        );

        // DTO 변환
        List<InAppNotificationResponse> content = notificationPage.getContent().stream()
                .map(this::toInAppNotificationResponse)
                .collect(Collectors.toList());

        return InAppNotificationListResponse.builder()
                .content(content)
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .build();
    }

    /**
     * Notification 엔티티를 InAppNotificationResponse DTO로 변환
     */
    private InAppNotificationResponse toInAppNotificationResponse(Notification notification) {
        return InAppNotificationResponse.builder()
                .notificationId(notification.getId().toString())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .category(notification.getType())
                .linkUrl(notification.getLinkUrl())
                .read(notification.isRead())
                .createdAt(formatToISO8601(notification.getCreatedAt()))
                .readAt(notification.getReadAt() != null ? formatToISO8601(notification.getReadAt()) : null)
                .build();
    }

    /**
     * LocalDateTime을 ISO-8601 형식 문자열로 변환
     */
    private String formatToISO8601(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atOffset(ZoneOffset.UTC).format(ISO_FORMATTER);
    }
}

