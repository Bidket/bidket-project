package com.bidket.notification.application.service;

import com.bidket.notification.domain.model.NotificationChannel;
import com.bidket.notification.domain.model.NotificationStatus;
import com.bidket.notification.domain.exception.NotificationErrorCode;
import com.bidket.notification.domain.exception.NotificationException;
import com.bidket.notification.infrastructure.external.SlackSender;
import com.bidket.notification.infrastructure.persistence.entity.Notification;
import com.bidket.notification.infrastructure.persistence.repository.NotificationRepository;
import com.bidket.notification.presentation.dto.request.SendNotificationRequest;
import com.bidket.notification.presentation.dto.response.InAppNotificationListResponse;
import com.bidket.notification.presentation.dto.response.InAppNotificationResponse;
import com.bidket.notification.presentation.dto.response.SendNotificationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 알림 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SlackSender slackSender;
    private final ObjectMapper objectMapper;

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
                .category(notification.getCategory())
                .linkUrl(notification.getLinkUrl())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }

    /**
     * 알림 단건 발송
     *
     * @param request 알림 발송 요청
     * @return 알림 발송 응답
     */
    @Transactional
    public SendNotificationResponse sendNotification(SendNotificationRequest request) {
        // 1. NotificationChannel 변환
        NotificationChannel channel;
        try {
            channel = NotificationChannel.valueOf(request.type().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_TYPE);
        }

        // 2. payload를 JSON 문자열로 변환
        String payloadJson = null;
        if (request.payload() != null && !request.payload().isEmpty()) {
            try {
                payloadJson = objectMapper.writeValueAsString(request.payload());
            } catch (Exception e) {
                log.warn("payload JSON 변환 실패: {}", e.getMessage());
            }
        }

        // 3. Notification 엔티티 생성 및 저장 (DB 저장이 성공해야만 Slack 발송 진행)
        // type은 기존 호환성을 위해 category 값을 사용 (둘은 독립적으로 관리되지만 DB 스키마 호환성 유지)
        String typeValue = request.category() != null ? request.category() : "SYSTEM";
        Notification notification = Notification.builder()
                .userId(request.userId())
                .type(typeValue) // DB 호환성을 위해 category 값을 type에도 저장
                .category(request.category())
                .channel(channel)
                .title(request.title())
                .message(request.message())
                .linkUrl(request.linkUrl())
                .payload(payloadJson)
                .status(NotificationStatus.PENDING)
                .build();

        // DB 저장 시도 (실패하면 예외 발생하여 Slack 발송하지 않음)
        try {
            notification = notificationRepository.save(notification);
            // flush를 명시적으로 호출하여 DB 제약조건 검증
            notificationRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("알림 저장 실패 (DB 제약조건 위반): {}", e.getMessage(), e);
            // user_id NOT NULL 제약조건 위반인 경우 더 명확한 메시지 제공
            if (e.getMessage() != null && e.getMessage().contains("user_id")) {
                throw new NotificationException(NotificationErrorCode.INTERNAL_SERVER_ERROR);
            }
            throw new NotificationException(NotificationErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("알림 저장 실패: {}", e.getMessage(), e);
            throw new NotificationException(NotificationErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 4. DB 저장 성공 후 Slack 알림 발송 처리
        if (channel == NotificationChannel.SLACK) {
            return sendSlackNotification(notification, request);
        }

        // 5. 다른 채널은 아직 미구현
        notification.markAsFailed();
        notificationRepository.save(notification);
        throw new NotificationException(NotificationErrorCode.NOTIFICATION_CHANNEL_NOT_SUPPORTED);
    }

    /**
     * Slack 알림 발송
     * DB 저장이 성공한 후에만 호출됨
     */
    private SendNotificationResponse sendSlackNotification(Notification notification, SendNotificationRequest request) {
        try {
            // Slack 메시지 발송 (DB 저장 성공 후에만 실행)
            boolean success = slackSender.sendMessage(
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getLinkUrl()
            );

            if (success) {
                notification.markAsSent();
                notificationRepository.save(notification);
                return SendNotificationResponse.builder()
                        .notificationId(notification.getId())
                        .status(NotificationStatus.SENT.name())
                        .type(request.type())
                        .message("알림이 정상적으로 발송되었습니다.")
                        .build();
            } else {
                notification.markAsFailed();
                notificationRepository.save(notification);
                return SendNotificationResponse.builder()
                        .notificationId(notification.getId())
                        .status(NotificationStatus.FAILED.name())
                        .type(request.type())
                        .message("Slack 메시지 발송에 실패했습니다.")
                        .build();
            }
        } catch (Exception e) {
            log.error("Slack 알림 발송 중 오류 발생: {}", e.getMessage(), e);
            // DB 저장은 이미 성공했으므로 실패 상태로 업데이트
            try {
                notification.markAsFailed();
                notificationRepository.save(notification);
            } catch (Exception dbException) {
                log.error("알림 상태 업데이트 실패: {}", dbException.getMessage(), dbException);
            }
            return SendNotificationResponse.builder()
                    .notificationId(notification.getId())
                    .status(NotificationStatus.FAILED.name())
                    .type(request.type())
                    .message("Slack 메시지 발송 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }
}

