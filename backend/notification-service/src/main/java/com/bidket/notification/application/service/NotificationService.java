package com.bidket.notification.application.service;

import com.bidket.notification.domain.exception.NotificationErrorCode;
import com.bidket.notification.domain.exception.NotificationException;
import com.bidket.notification.domain.model.NotificationChannel;
import com.bidket.notification.domain.model.NotificationStatus;
import com.bidket.notification.infrastructure.external.SlackSender;
import com.bidket.notification.infrastructure.persistence.entity.Notification;
import com.bidket.notification.infrastructure.persistence.repository.NotificationRepository;
import com.bidket.notification.presentation.dto.request.SendNotificationRequest;
import com.bidket.notification.presentation.dto.response.SendNotificationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 발송 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SlackSender slackSender;
    private final ObjectMapper objectMapper;

    /**
     * 알림 단건 발송
     * @param request 알림 발송 요청
     * @return 알림 발송 응답
     */
    @Transactional
    public SendNotificationResponse sendNotification(SendNotificationRequest request) {
        // Slack 타입만 지원
        if (request.type() == null || !request.type().equalsIgnoreCase("SLACK")) {
            throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_TYPE,
                    "현재 Slack 알림만 지원합니다.");
        }

        // payload를 JSON 문자열로 변환
        String payloadJson = null;
        if (request.payload() != null && !request.payload().isEmpty()) {
            try {
                payloadJson = objectMapper.writeValueAsString(request.payload());
            } catch (JsonProcessingException e) {
                log.warn("Payload JSON 변환 실패: {}", e.getMessage());
            }
        }

        // 알림 엔티티 생성
        Notification notification = Notification.builder()
                .userId(request.userId())
                .type(request.type())
                .category(request.category())
                .channel(NotificationChannel.SLACK)
                .title(request.title())
                .message(request.message())
                .linkUrl(request.linkUrl())
                .payload(payloadJson)
                .status(NotificationStatus.PENDING)
                .build();

        // 알림 저장
        notification = notificationRepository.save(notification);

        // Slack 발송 처리
        NotificationStatus finalStatus;
        String resultMessage;
        
        try {
            // Slack 알림은 항상 기본 webhook URL 사용 (target 필드 무시)
            boolean success = slackSender.sendMessage(
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getLinkUrl()
            );

            if (success) {
                notification.markAsSent();
                finalStatus = NotificationStatus.SENT;
                resultMessage = "알림이 정상적으로 발송되었습니다.";
            } else {
                notification.markAsFailed();
                finalStatus = NotificationStatus.FAILED;
                resultMessage = "알림 발송에 실패했습니다.";
            }
            
            notification = notificationRepository.save(notification);
            
        } catch (Exception e) {
            log.error("알림 발송 중 오류 발생: {}", e.getMessage(), e);
            notification.markAsFailed();
            notification = notificationRepository.save(notification);
            finalStatus = NotificationStatus.FAILED;
            resultMessage = "알림 발송 중 오류가 발생했습니다: " + e.getMessage();
        }

        return SendNotificationResponse.builder()
                .notificationId(notification.getId().toString())
                .status(finalStatus.name())
                .type(request.type())
                .message(resultMessage)
                .build();
    }
}

