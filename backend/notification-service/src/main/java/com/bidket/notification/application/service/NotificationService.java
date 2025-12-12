package com.bidket.notification.application.service;

import com.bidket.notification.domain.exception.NotificationErrorCode;
import com.bidket.notification.domain.exception.NotificationException;
import com.bidket.notification.domain.model.NotificationChannel;
import com.bidket.notification.domain.model.NotificationStatus;
import com.bidket.notification.infrastructure.external.EmailSender;
import com.bidket.notification.infrastructure.external.SlackSender;
import com.bidket.notification.infrastructure.persistence.entity.Notification;
import com.bidket.notification.infrastructure.persistence.repository.NotificationRepository;
import com.bidket.notification.global.security.AuthenticationHelper;
import com.bidket.notification.presentation.dto.request.SendNotificationRequest;
import com.bidket.notification.presentation.dto.response.DeleteNotificationResponse;
import com.bidket.notification.presentation.dto.response.ReadNotificationResponse;
import com.bidket.notification.presentation.dto.response.SendNotificationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
            case "IN_APP":
                channel = NotificationChannel.IN_APP;
                // In-App 알림의 경우 userId가 필수
                if (request.userId() == null) {
                    throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_TYPE,
                            "In-App 알림의 경우 대상 회원 ID(userId)가 필수입니다.");
                }
                break;
            default:
                throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_TYPE,
                        "지원하지 않는 알림 타입입니다. (지원 타입: SLACK, EMAIL, IN_APP)");
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
            } else if (notificationType.equals("IN_APP")) {
                // In-App 알림은 외부 발송이 필요 없으므로 바로 성공 처리
                // 데이터베이스에 저장된 알림을 사용자가 조회할 수 있도록 함
                success = true;
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

    /**
     * 인앱 알림 읽음 처리
     * @param notificationId 알림 ID
     * @return 읽음 처리 응답
     */
    @Transactional
    public ReadNotificationResponse markNotificationAsRead(UUID notificationId) {
        // 현재 사용자 ID 추출
        UUID currentUserId = AuthenticationHelper.getCurrentUserId();

        // 알림 조회
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND,
                        "알림을 찾을 수 없습니다."));

        // In-App 알림인지 확인
        if (notification.getChannel() != NotificationChannel.IN_APP) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_CHANNEL_NOT_SUPPORTED,
                    "In-App 알림만 읽음 처리가 가능합니다.");
        }

        // 본인 소유 알림인지 확인
        if (!currentUserId.equals(notification.getUserId())) {
            throw new NotificationException(NotificationErrorCode.FORBIDDEN,
                    "본인 소유 알림에 대해서만 읽음 처리가 가능합니다.");
        }

        // 읽음 처리
        notification.markAsRead();
        notification = notificationRepository.save(notification);

        return ReadNotificationResponse.builder()
                .notificationId(notification.getId().toString())
                .read(true)
                .readAt(notification.getReadAt())
                .build();
    }

    /**
     * 인앱 알림 삭제 처리
     * @param notificationId 알림 ID
     * @return 삭제 처리 응답
     */
    @Transactional
    public DeleteNotificationResponse deleteNotification(UUID notificationId) {
        // 현재 사용자 ID 추출
        UUID currentUserId = AuthenticationHelper.getCurrentUserId();

        // 알림 조회
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND,
                        "알림을 찾을 수 없습니다."));

        // In-App 알림인지 확인
        if (notification.getChannel() != NotificationChannel.IN_APP) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_CHANNEL_NOT_SUPPORTED,
                    "In-App 알림만 삭제가 가능합니다.");
        }

        // 본인 소유 알림인지 확인
        if (!currentUserId.equals(notification.getUserId())) {
            throw new NotificationException(NotificationErrorCode.FORBIDDEN,
                    "본인 소유 알림에 대해서만 삭제가 가능합니다.");
        }

        // 이미 삭제된 알림인지 확인
        if (notification.isDeleted()) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND,
                    "이미 삭제된 알림입니다.");
        }

        // 삭제 처리 (soft delete)
        // BaseEntity의 markDeleted() 메서드 사용 (deletedBy는 null로 설정)
        notification.markDeleted(null);
        notificationRepository.save(notification);

        return DeleteNotificationResponse.builder()
                .success(true)
                .message("알림이 삭제되었습니다.")
                .build();
    }
}

