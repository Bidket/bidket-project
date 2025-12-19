package com.bidket.notification.application.service;

import com.bidket.notification.domain.model.EventTemplate;
import com.bidket.notification.domain.model.NotificationCategory;
import com.bidket.notification.domain.model.NotificationChannel;
import com.bidket.notification.infrastructure.external.AuctionServiceClient;
import com.bidket.notification.infrastructure.external.EmailSender;
import com.bidket.notification.infrastructure.external.SlackSender;
import com.bidket.notification.infrastructure.external.UserServiceClient;
import com.bidket.notification.infrastructure.persistence.entity.Notification;
import com.bidket.notification.infrastructure.persistence.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Kafka 이벤트 기반 알림 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventNotificationService {

    private final NotificationRepository notificationRepository;
    private final AuctionServiceClient auctionServiceClient;
    // 추후 EMAIL/SLACK 채널 지원 시 사용 예정
    @SuppressWarnings("unused")
    private final UserServiceClient userServiceClient;
    @SuppressWarnings("unused")
    private final EmailSender emailSender;
    @SuppressWarnings("unused")
    private final SlackSender slackSender;
    private final ObjectMapper objectMapper;

    /**
     * 이벤트 처리
     * @param eventTemplate Kafka 이벤트 템플릿
     * @param topic Kafka topic 이름 (category 추출용)
     */
    @Transactional
    public void processEvent(EventTemplate eventTemplate, String topic) {
        // 필수 필드 검증
        if (eventTemplate.eventId() == null) {
            log.error("eventId가 null입니다. topic={}", topic);
            throw new IllegalArgumentException("eventId는 필수입니다");
        }
        if (eventTemplate.userId() == null) {
            log.error("userId가 null입니다. eventId={}, topic={}", eventTemplate.eventId(), topic);
            throw new IllegalArgumentException("userId는 필수입니다");
        }

        // MDC 설정 (로깅용) - null-safe
        MDC.put("eventId", eventTemplate.eventId().toString());
        MDC.put("userId", eventTemplate.userId().toString());
        
        try {
            // 1. topic에서 category 추출 (멱등성 체크 전에 category 필요)
            NotificationCategory category = extractCategoryFromTopic(topic);
            if (category == null) {
                log.error("지원하지 않는 topic입니다. topic={}, eventId={}", topic, eventTemplate.eventId());
                throw new IllegalArgumentException("지원하지 않는 topic입니다: " + topic);
            }

            // 3. 이벤트 타입별 처리 (각 이벤트 처리 메서드에서 채널별 멱등성 체크 수행)
            if ("near_turn".equals(eventTemplate.eventType())) {
                processNearTurnEvent(eventTemplate, category);
            } else {
                log.error("지원하지 않는 이벤트 타입입니다. eventType={}, eventId={}", 
                        eventTemplate.eventType(), eventTemplate.eventId());
                throw new IllegalArgumentException("지원하지 않는 이벤트 타입입니다: " + eventTemplate.eventType());
            }

            log.info("이벤트 처리 완료: eventId={}, eventType={}", 
                    eventTemplate.eventId(), eventTemplate.eventType());

        } catch (Exception e) {
            log.error("이벤트 처리 중 오류 발생: eventId={}, eventType={}, error={}", 
                    eventTemplate.eventId(), eventTemplate.eventType(), e.getMessage(), e);
            throw e; // Kafka 재시도를 위해 예외 전파
        } finally {
            MDC.clear();
        }
    }

    /**
     * topic에서 category 추출
     * notification.queue.* -> QUEUE
     * notification.auction.* -> AUCTION
     * notification.order.* -> ORDER
     */
    private NotificationCategory extractCategoryFromTopic(String topic) {
        if (topic == null || topic.isEmpty()) {
            return null;
        }
        if (topic.startsWith("notification.queue")) {
            return NotificationCategory.QUEUE;
        } else if (topic.startsWith("notification.auction")) {
            return NotificationCategory.AUCTION;
        } else if (topic.startsWith("notification.order")) {
            return NotificationCategory.ORDER;
        }
        return null;
    }

    /**
     * 대기 순번 임박 알림 처리 (near_turn)
     */
    private void processNearTurnEvent(EventTemplate eventTemplate, NotificationCategory category) {
        // 멱등성 확인: IN_APP 채널 기준 (UNIQUE(event_id, channel))
        if (notificationRepository.existsByEventIdAndChannel(
                eventTemplate.eventId(), NotificationChannel.IN_APP)) {
            log.info("이미 처리된 이벤트입니다. eventId={}, channel=IN_APP", eventTemplate.eventId());
            return;
        }

        Map<String, Object> data = eventTemplate.data();
        if (data == null) {
            log.error("near_turn 이벤트의 data가 null입니다. eventId={}", eventTemplate.eventId());
            throw new IllegalArgumentException("near_turn 이벤트의 data가 null입니다");
        }

        // 필수 필드 추출
        UUID auctionId = extractUUID(data, "auctionId");
        // queue-service는 rank 필드를 사용하므로 둘 다 지원
        Integer position = extractInteger(data, "position");
        if (position == null) {
            position = extractInteger(data, "rank");
        }

        if (auctionId == null || position == null) {
            log.error("near_turn 이벤트의 필수 필드가 누락되었습니다. eventId={}, auctionId={}, position={}", 
                    eventTemplate.eventId(), auctionId, position);
            throw new IllegalArgumentException(
                    String.format("near_turn 이벤트의 필수 필드가 누락되었습니다. auctionId=%s, position=%s", 
                            auctionId, position));
        }

        MDC.put("auctionId", auctionId.toString());

        // 경매 정보 조회 (실패 시 null 반환 가능)
        // 추후 경매명을 메시지에 포함할 때 사용 예정
        AuctionServiceClient.AuctionInfo auctionInfo = auctionServiceClient.getAuctionInfo(auctionId);
        
        // 경매 정보 조회 실패 시에도 알림 발송은 계속 진행 (기본 메시지 사용)
        if (auctionInfo == null) {
            log.warn("경매 정보 조회 실패: auctionId={}, 기본 메시지로 알림 발송", auctionId);
        }

        // 알림 메시지 생성
        // 추후 auctionInfo가 null이 아닐 때 경매명을 포함하도록 확장 가능
        String title = "대기 순번 임박";
        String message = String.format("현재 대기 순번은 **%d번**입니다. 잠시 후 입장하실 수 있습니다.", position);

        // payload 생성
        String payload = createPayload(data);

        // IN_APP 알림 저장 및 발송
        // IN_APP 알림은 외부 발송이 필요 없으므로 DB 저장 시점이 발송 시점
        Notification inAppNotification = Notification.builder()
                .userId(eventTemplate.userId())
                .eventId(eventTemplate.eventId()) // 멱등성 처리용 (UNIQUE(event_id, channel))
                .type(eventTemplate.eventType()) // Kafka eventType (near_turn, admitted, outbid, closed, payment_required, paid)
                .category(category.name()) // Kafka topic 기반 카테고리 (QUEUE, AUCTION, ORDER)
                .channel(NotificationChannel.IN_APP)
                .title(title)
                .message(message)
                .linkUrl(null)
                .payload(payload)
                .occurredAt(eventTemplate.occurredAt()) // 이벤트 발생 시각
                .source(eventTemplate.source()) // 이벤트 발생 서비스
                .retryCount(0) // 초기 재시도 횟수
                .build();

        // markAsSent()에서 status=SENT, sentAt=현재시각 설정
        inAppNotification.markAsSent();
        notificationRepository.save(inAppNotification);

        log.info("대기 순번 임박 알림 발송 완료: userId={}, auctionId={}, position={}", 
                eventTemplate.userId(), auctionId, position);
    }

    /**
     * UUID 추출 헬퍼 메서드
     */
    private UUID extractUUID(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof UUID) {
            return (UUID) value;
        }
        if (value instanceof String) {
            try {
                return UUID.fromString((String) value);
            } catch (IllegalArgumentException e) {
                log.warn("UUID 변환 실패: key={}, value={}", key, value);
                return null;
            }
        }
        return null;
    }

    /**
     * Integer 추출 헬퍼 메서드
     */
    private Integer extractInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("Integer 변환 실패: key={}, value={}", key, value);
                return null;
            }
        }
        return null;
    }

    /**
     * Payload JSON 문자열 생성
     */
    private String createPayload(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Payload JSON 변환 실패: {}", e.getMessage());
            return "{}";
        }
    }
}

