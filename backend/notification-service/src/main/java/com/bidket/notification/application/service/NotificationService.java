package com.bidket.notification.application.service;

import com.bidket.notification.domain.exception.NotificationErrorCode;
import com.bidket.notification.domain.exception.NotificationException;
import com.bidket.notification.domain.model.NotificationChannel;
import com.bidket.notification.domain.model.NotificationStatus;
import com.bidket.notification.infrastructure.external.EmailSender;
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
    private final EmailSender emailSender;
    private final ObjectMapper objectMapper;

    /**
     * 알림 단건 발송
     * @param request 알림 발송 요청
     * @return 알림 발송 응답
     */
    @Transactional
    public SendNotificationResponse sendNotification(SendNotificationRequest request) {
        // 알림 타입 검증
        if (request.type() == null) {
            throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_TYPE,
                    "알림 타입은 필수입니다.");
        }

        String notificationType = request.type().toUpperCase();
        NotificationChannel channel;

        // 알림 타입에 따른 채널 설정
        switch (notificationType) {
            case "SLACK":
                channel = NotificationChannel.SLACK;
                break;
            case "EMAIL":
                channel = NotificationChannel.EMAIL;
                // 이메일 타입일 경우 target 필드에 이메일 주소가 필요
                if (request.target() == null || request.target().trim().isEmpty()) {
                    throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_TYPE,
                            "이메일 알림의 경우 수신자 이메일 주소(target)가 필수입니다.");
                }
                break;
            default:
                throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_TYPE,
                        "지원하지 않는 알림 타입입니다. (지원 타입: SLACK, EMAIL)");
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
                .channel(channel)
                .title(request.title())
                .message(request.message())
                .linkUrl(request.linkUrl())
                .payload(payloadJson)
                .status(NotificationStatus.PENDING)
                .build();

        // 알림 저장
        notification = notificationRepository.save(notification);

        // 알림 타입에 따른 발송 처리
        NotificationStatus finalStatus;
        String resultMessage;
        
        try {
            boolean success = false;

            if (notificationType.equals("SLACK")) {
                // Slack 발송 처리
                success = slackSender.sendMessage(
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.getLinkUrl()
                );
            } else if (notificationType.equals("EMAIL")) {
                // 이메일 발송 처리 (HTML 형식)
                success = emailSender.sendHtmlEmail(
                        request.target(), // 수신자 이메일 주소
                        notification.getTitle(),
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.getLinkUrl()
                );
            }

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

