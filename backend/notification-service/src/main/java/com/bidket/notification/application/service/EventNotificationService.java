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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final UserServiceClient userServiceClient;
    private final EmailSender emailSender;
    @SuppressWarnings("unused")
    private final SlackSender slackSender; // 추후 운영/모니터링용으로 사용 예정
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
        // eventType null/blank 체크 추가
        String eventType = eventTemplate.eventType();
        if (eventType == null || eventType.isBlank()) {
            log.error("eventType이 null이거나 비어있습니다. eventId={}, topic={}", eventTemplate.eventId(), topic);
            throw new IllegalArgumentException("eventType은 필수입니다");
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
            switch (eventType) {
                case "near_turn" -> processNearTurnEvent(eventTemplate, category);
                case "admitted" -> processAdmittedEvent(eventTemplate, category);
                case "outbid" -> processOutbidEvent(eventTemplate, category);
                case "closed" -> processClosedEvent(eventTemplate, category);
                case "payment_required" -> processPaymentRequiredEvent(eventTemplate, category);
                case "paid" -> processPaidEvent(eventTemplate, category);
                default -> {
                    log.error("지원하지 않는 이벤트 타입입니다. eventType={}, eventId={}", 
                            eventType, eventTemplate.eventId());
                    throw new IllegalArgumentException("지원하지 않는 이벤트 타입입니다: " + eventType);
                }
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

        // 경매 정보 조회 (실패해도 진행 - try/catch로 처리)
        try {
            AuctionServiceClient.AuctionInfo auctionInfo = auctionServiceClient.getAuctionInfo(auctionId);
            if (auctionInfo == null) {
                log.warn("경매 정보 조회 실패: auctionId={}, 기본 메시지로 알림 발송", auctionId);
            }
        } catch (Exception e) {
            log.warn("경매 정보 조회 중 오류 발생: auctionId={}, 기본 메시지로 알림 발송, error={}", 
                    auctionId, e.getMessage());
        }

        String title = "대기 순번 임박";
        String message = String.format("현재 대기 순번은 **%d번**입니다. 잠시 후 입장하실 수 있습니다.", position);

        saveAndSendNotification(eventTemplate, category, NotificationChannel.IN_APP, 
                title, message, null, data);

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

    /**
     * LocalDateTime 추출 헬퍼 메서드
     */
    private LocalDateTime extractLocalDateTime(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof String) {
            try {
                // ISO-8601 형식 파싱
                return LocalDateTime.parse((String) value);
            } catch (Exception e) {
                log.warn("LocalDateTime 변환 실패: key={}, value={}", key, value);
                return null;
            }
        }
        return null;
    }

    /**
     * Long 추출 헬퍼 메서드 (가격 필드용)
     */
    private Long extractLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("Long 변환 실패: key={}, value={}", key, value);
                return null;
            }
        }
        return null;
    }

    /**
     * String 추출 헬퍼 메서드
     */
    private String extractString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * 공통 알림 저장 및 발송 로직
     */
    private void saveAndSendNotification(
            EventTemplate eventTemplate,
            NotificationCategory category,
            NotificationChannel channel,
            String title,
            String message,
            String linkUrl,
            Map<String, Object> data
    ) {
        // 멱등성 확인: 채널별로 이미 처리된 이벤트인지 확인
        if (notificationRepository.existsByEventIdAndChannel(eventTemplate.eventId(), channel)) {
            log.info("이미 처리된 이벤트입니다. eventId={}, channel={}", eventTemplate.eventId(), channel);
            return;
        }

        String payload = createPayload(data);

        Notification notification = Notification.builder()
                .userId(eventTemplate.userId())
                .eventId(eventTemplate.eventId())
                .type(eventTemplate.eventType())
                .category(category.name())
                .channel(channel)
                .title(title)
                .message(message)
                .linkUrl(linkUrl)
                .payload(payload)
                .occurredAt(eventTemplate.occurredAt())
                .source(eventTemplate.source())
                .retryCount(0)
                .build();

        // 채널별 발송 처리
        if (channel == NotificationChannel.IN_APP) {
            // IN_APP은 DB 저장 시점이 발송 시점
            notification.markAsSent();
        } else if (channel == NotificationChannel.EMAIL) {
            // EMAIL은 외부 발송 후 상태 업데이트
            String userEmail = null;
            try {
                userEmail = userServiceClient.getUserEmail(eventTemplate.userId());
            } catch (Exception e) {
                log.warn("사용자 이메일 조회 실패: userId={}, EMAIL 채널 스킵, error={}", 
                        eventTemplate.userId(), e.getMessage());
            }
            
            if (userEmail == null || userEmail.trim().isEmpty()) {
                log.warn("사용자 이메일 조회 실패: userId={}, EMAIL 채널 스킵", eventTemplate.userId());
                notification.markAsSkipped();
            } else {
                boolean sent = false;
                try {
                    sent = emailSender.sendHtmlEmail(userEmail, title, title, message, linkUrl);
                } catch (Exception e) {
                    log.error("이메일 발송 중 오류 발생: userId={}, email={}, error={}", 
                            eventTemplate.userId(), userEmail, e.getMessage(), e);
                }
                
                if (sent) {
                    notification.markAsSent();
                } else {
                    // Kafka 재시도형이므로 EMAIL 실패 시 예외를 던져서 재시도 트리거
                    notification.markAsFailed("EMAIL_SEND_FAILED", "이메일 발송 실패");
                    throw new RuntimeException("이메일 발송 실패: userId=" + eventTemplate.userId() + 
                            ", email=" + userEmail);
                }
            }
        }

        try {
            notificationRepository.save(notification);
        } catch (DataIntegrityViolationException e) {
            // unique violation(중복)은 예외로 재시도하지 말고 skip
            log.info("중복 알림 감지 (unique constraint violation): eventId={}, channel={}, 이미 처리된 것으로 간주하고 skip", 
                    eventTemplate.eventId(), channel);
            // 예외를 던지지 않고 정상 종료 (재시도 방지)
        }
    }

    /**
     * 경매 참여 가능 알림 처리 (admitted)
     */
    private void processAdmittedEvent(EventTemplate eventTemplate, NotificationCategory category) {
        Map<String, Object> data = eventTemplate.data();
        if (data == null) {
            log.error("admitted 이벤트의 data가 null입니다. eventId={}", eventTemplate.eventId());
            throw new IllegalArgumentException("admitted 이벤트의 data가 null입니다");
        }

        UUID auctionId = extractUUID(data, "auctionId");
        LocalDateTime admittedAt = extractLocalDateTime(data, "admittedAt");

        if (auctionId == null || admittedAt == null) {
            log.error("admitted 이벤트의 필수 필드가 누락되었습니다. eventId={}, auctionId={}, admittedAt={}", 
                    eventTemplate.eventId(), auctionId, admittedAt);
            throw new IllegalArgumentException(
                    String.format("admitted 이벤트의 필수 필드가 누락되었습니다. auctionId=%s, admittedAt=%s", 
                            auctionId, admittedAt));
        }

        MDC.put("auctionId", auctionId.toString());

        // 경매 정보 조회 (실패해도 진행 - try/catch로 처리)
        try {
            AuctionServiceClient.AuctionInfo auctionInfo = auctionServiceClient.getAuctionInfo(auctionId);
            if (auctionInfo == null) {
                log.warn("경매 정보 조회 실패: auctionId={}, 기본 메시지로 알림 발송", auctionId);
            }
        } catch (Exception e) {
            log.warn("경매 정보 조회 중 오류 발생: auctionId={}, 기본 메시지로 알림 발송, error={}", 
                    auctionId, e.getMessage());
        }

        String title = "입장 가능";
        String message = "지금 입장해 입찰에 참여하세요.";

        saveAndSendNotification(eventTemplate, category, NotificationChannel.IN_APP, 
                title, message, null, data);

        log.info("경매 참여 가능 알림 발송 완료: userId={}, auctionId={}", 
                eventTemplate.userId(), auctionId);
    }

    /**
     * 상회 입찰 알림 처리 (outbid)
     */
    private void processOutbidEvent(EventTemplate eventTemplate, NotificationCategory category) {
        Map<String, Object> data = eventTemplate.data();
        if (data == null) {
            log.error("outbid 이벤트의 data가 null입니다. eventId={}", eventTemplate.eventId());
            throw new IllegalArgumentException("outbid 이벤트의 data가 null입니다");
        }

        UUID auctionId = extractUUID(data, "auctionId");
        Long currentPrice = extractLong(data, "currentPrice");
        LocalDateTime outbidAt = extractLocalDateTime(data, "outbidAt");

        if (auctionId == null || currentPrice == null || outbidAt == null) {
            log.error("outbid 이벤트의 필수 필드가 누락되었습니다. eventId={}, auctionId={}, currentPrice={}, outbidAt={}", 
                    eventTemplate.eventId(), auctionId, currentPrice, outbidAt);
            throw new IllegalArgumentException(
                    String.format("outbid 이벤트의 필수 필드가 누락되었습니다. auctionId=%s, currentPrice=%s, outbidAt=%s", 
                            auctionId, currentPrice, outbidAt));
        }

        MDC.put("auctionId", auctionId.toString());

        // 경매 정보 조회 (실패해도 진행 - try/catch로 처리)
        try {
            AuctionServiceClient.AuctionInfo auctionInfo = auctionServiceClient.getAuctionInfo(auctionId);
            if (auctionInfo == null) {
                log.warn("경매 정보 조회 실패: auctionId={}, 기본 메시지로 알림 발송", auctionId);
            }
        } catch (Exception e) {
            log.warn("경매 정보 조회 중 오류 발생: auctionId={}, 기본 메시지로 알림 발송, error={}", 
                    auctionId, e.getMessage());
        }

        String title = "상회 입찰 발생";
        String message = "다른 사용자가 더 높은 금액으로 입찰했습니다.";

        saveAndSendNotification(eventTemplate, category, NotificationChannel.IN_APP, 
                title, message, null, data);

        log.info("상회 입찰 알림 발송 완료: userId={}, auctionId={}, currentPrice={}", 
                eventTemplate.userId(), auctionId, currentPrice);
    }

    /**
     * 경매 종료 알림 처리 (closed)
     */
    private void processClosedEvent(EventTemplate eventTemplate, NotificationCategory category) {
        Map<String, Object> data = eventTemplate.data();
        if (data == null) {
            log.error("closed 이벤트의 data가 null입니다. eventId={}", eventTemplate.eventId());
            throw new IllegalArgumentException("closed 이벤트의 data가 null입니다");
        }

        UUID auctionId = extractUUID(data, "auctionId");
        String result = extractString(data, "result");
        Long finalPrice = extractLong(data, "finalPrice");
        LocalDateTime closedAt = extractLocalDateTime(data, "closedAt");

        if (auctionId == null || result == null || finalPrice == null || closedAt == null) {
            log.error("closed 이벤트의 필수 필드가 누락되었습니다. eventId={}, auctionId={}, result={}, finalPrice={}, closedAt={}", 
                    eventTemplate.eventId(), auctionId, result, finalPrice, closedAt);
            throw new IllegalArgumentException(
                    String.format("closed 이벤트의 필수 필드가 누락되었습니다. auctionId=%s, result=%s, finalPrice=%s, closedAt=%s", 
                            auctionId, result, finalPrice, closedAt));
        }

        // result 검증 (WON 또는 LOST)
        if (!"WON".equals(result) && !"LOST".equals(result)) {
            log.error("closed 이벤트의 result가 유효하지 않습니다. eventId={}, result={}", 
                    eventTemplate.eventId(), result);
            throw new IllegalArgumentException("result는 WON 또는 LOST여야 합니다: " + result);
        }

        MDC.put("auctionId", auctionId.toString());

        // 경매 정보 조회 (실패해도 진행 - try/catch로 처리)
        try {
            AuctionServiceClient.AuctionInfo auctionInfo = auctionServiceClient.getAuctionInfo(auctionId);
            if (auctionInfo == null) {
                log.warn("경매 정보 조회 실패: auctionId={}, 기본 메시지로 알림 발송", auctionId);
            }
        } catch (Exception e) {
            log.warn("경매 정보 조회 중 오류 발생: auctionId={}, 기본 메시지로 알림 발송, error={}", 
                    auctionId, e.getMessage());
        }

        String title = "경매 종료";
        String message = String.format("결과: **%s**, 낙찰가: **%d원**", result, finalPrice);

        saveAndSendNotification(eventTemplate, category, NotificationChannel.IN_APP, 
                title, message, null, data);

        log.info("경매 종료 알림 발송 완료: userId={}, auctionId={}, result={}, finalPrice={}", 
                eventTemplate.userId(), auctionId, result, finalPrice);
    }

    /**
     * 결제 필요 알림 처리 (payment_required)
     */
    private void processPaymentRequiredEvent(EventTemplate eventTemplate, NotificationCategory category) {
        Map<String, Object> data = eventTemplate.data();
        if (data == null) {
            log.error("payment_required 이벤트의 data가 null입니다. eventId={}", eventTemplate.eventId());
            throw new IllegalArgumentException("payment_required 이벤트의 data가 null입니다");
        }

        UUID orderId = extractUUID(data, "orderId");
        UUID auctionId = extractUUID(data, "auctionId");
        Long amount = extractLong(data, "amount");
        LocalDateTime payDueAt = extractLocalDateTime(data, "payDueAt");

        if (orderId == null || auctionId == null || amount == null || payDueAt == null) {
            log.error("payment_required 이벤트의 필수 필드가 누락되었습니다. eventId={}, orderId={}, auctionId={}, amount={}, payDueAt={}", 
                    eventTemplate.eventId(), orderId, auctionId, amount, payDueAt);
            throw new IllegalArgumentException(
                    String.format("payment_required 이벤트의 필수 필드가 누락되었습니다. orderId=%s, auctionId=%s, amount=%s, payDueAt=%s", 
                            orderId, auctionId, amount, payDueAt));
        }

        MDC.put("orderId", orderId.toString());
        MDC.put("auctionId", auctionId.toString());

        // 경매 정보 조회 (실패해도 진행 - try/catch로 처리)
        try {
            AuctionServiceClient.AuctionInfo auctionInfo = auctionServiceClient.getAuctionInfo(auctionId);
            if (auctionInfo == null) {
                log.warn("경매 정보 조회 실패: auctionId={}, 기본 메시지로 알림 발송", auctionId);
            }
        } catch (Exception e) {
            log.warn("경매 정보 조회 중 오류 발생: auctionId={}, 기본 메시지로 알림 발송, error={}", 
                    auctionId, e.getMessage());
        }

        String title = "결제 필요";
        // payDueAt을 포맷팅 (예: 2024-01-15 14:30)
        String formattedDueAt = payDueAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String message = String.format("**%s**까지 결제를 완료해주세요.", formattedDueAt);

        // IN_APP 알림 발송
        saveAndSendNotification(eventTemplate, category, NotificationChannel.IN_APP, 
                title, message, null, data);

        // EMAIL 알림 발송
        saveAndSendNotification(eventTemplate, category, NotificationChannel.EMAIL, 
                title, message, null, data);

        log.info("결제 필요 알림 발송 완료: userId={}, orderId={}, auctionId={}, amount={}", 
                eventTemplate.userId(), orderId, auctionId, amount);
    }

    /**
     * 결제 완료 알림 처리 (paid)
     */
    private void processPaidEvent(EventTemplate eventTemplate, NotificationCategory category) {
        Map<String, Object> data = eventTemplate.data();
        if (data == null) {
            log.error("paid 이벤트의 data가 null입니다. eventId={}", eventTemplate.eventId());
            throw new IllegalArgumentException("paid 이벤트의 data가 null입니다");
        }

        UUID orderId = extractUUID(data, "orderId");
        UUID auctionId = extractUUID(data, "auctionId");
        Long amount = extractLong(data, "amount");
        LocalDateTime paidAt = extractLocalDateTime(data, "paidAt");

        if (orderId == null || auctionId == null || amount == null || paidAt == null) {
            log.error("paid 이벤트의 필수 필드가 누락되었습니다. eventId={}, orderId={}, auctionId={}, amount={}, paidAt={}", 
                    eventTemplate.eventId(), orderId, auctionId, amount, paidAt);
            throw new IllegalArgumentException(
                    String.format("paid 이벤트의 필수 필드가 누락되었습니다. orderId=%s, auctionId=%s, amount=%s, paidAt=%s", 
                            orderId, auctionId, amount, paidAt));
        }

        MDC.put("orderId", orderId.toString());
        MDC.put("auctionId", auctionId.toString());

        // 경매 정보 조회 (실패해도 진행 - try/catch로 처리)
        try {
            AuctionServiceClient.AuctionInfo auctionInfo = auctionServiceClient.getAuctionInfo(auctionId);
            if (auctionInfo == null) {
                log.warn("경매 정보 조회 실패: auctionId={}, 기본 메시지로 알림 발송", auctionId);
            }
        } catch (Exception e) {
            log.warn("경매 정보 조회 중 오류 발생: auctionId={}, 기본 메시지로 알림 발송, error={}", 
                    auctionId, e.getMessage());
        }

        String title = "결제 완료";
        String message = "주문이 정상적으로 완료되었습니다.";

        // IN_APP 알림 발송
        saveAndSendNotification(eventTemplate, category, NotificationChannel.IN_APP, 
                title, message, null, data);

        // EMAIL 알림 발송
        saveAndSendNotification(eventTemplate, category, NotificationChannel.EMAIL, 
                title, message, null, data);

        log.info("결제 완료 알림 발송 완료: userId={}, orderId={}, auctionId={}, amount={}", 
                eventTemplate.userId(), orderId, auctionId, amount);
    }
}

