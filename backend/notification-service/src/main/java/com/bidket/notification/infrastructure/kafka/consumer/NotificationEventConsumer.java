package com.bidket.notification.infrastructure.kafka.consumer;

import com.bidket.notification.application.service.EventNotificationService;
import com.bidket.notification.domain.model.EventTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka 이벤트 Consumer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final EventNotificationService eventNotificationService;

    /**
     * notification.queue.near_turn 토픽 리스너
     * - 대기 순번 임박 알림 이벤트 처리
     */
    @KafkaListener(
            topics = "notification.queue.near_turn",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeNearTurnEvent(
            @Payload EventTemplate eventTemplate,
            @Header(KafkaHeaders.RECEIVED_KEY) String userId,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("Kafka 이벤트 수신: topic={}, partition={}, offset={}, eventId={}, eventType={}, userId={}",
                topic, partition, offset, eventTemplate.eventId(), 
                eventTemplate.eventType() != null ? eventTemplate.eventType() : "near_turn", userId);

        try {
            // eventType이 없을 경우 topic 이름에서 추론
            String eventType = eventTemplate.eventType();
            if (eventType == null || eventType.isEmpty()) {
                if (topic.contains("near_turn")) {
                    eventType = "near_turn";
                } else if (topic.contains("admitted")) {
                    eventType = "admitted";
                }
            }

            // 이벤트 타입별 분기 처리
            if ("near_turn".equals(eventType)) {
                // eventType이 없는 경우를 대비해 eventTemplate에 eventType 추가
                EventTemplate enrichedEvent = enrichEventTemplate(eventTemplate, eventType);
                // topic에서 category 추출 (notification.queue.near_turn -> QUEUE)
                eventNotificationService.processEvent(enrichedEvent, topic);
            } else {
                log.error("지원하지 않는 이벤트 타입입니다. eventType={}, eventId={}", 
                        eventType, eventTemplate.eventId());
                throw new IllegalArgumentException("지원하지 않는 이벤트 타입입니다: " + eventType);
            }

            // 수동 커밋 (enable-auto-commit=false)
            // processEvent()가 예외 없이 정상 반환된 경우에만 커밋
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("이벤트 처리 실패: topic={}, partition={}, offset={}, eventId={}, error={}",
                    topic, partition, offset, eventTemplate.eventId(), e.getMessage(), e);
            // 예외를 다시 던져서 Kafka 재시도 트리거
            throw e;
        }
    }

    /**
     * eventType이 없는 경우를 대비해 EventTemplate을 보강
     */
    private EventTemplate enrichEventTemplate(EventTemplate original, String eventType) {
        if (original.eventType() != null && !original.eventType().isEmpty()) {
            return original;
        }
        return new EventTemplate(
                original.eventId(),
                eventType,
                original.occurredAt(),
                original.source(),
                original.userId(),
                original.data()
        );
    }
}

