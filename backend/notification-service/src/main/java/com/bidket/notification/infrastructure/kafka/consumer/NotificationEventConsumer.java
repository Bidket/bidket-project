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
     * notification.queue 토픽 리스너
     * - 대기 순번 임박 알림 (near_turn)
     * - 경매 참여 가능 알림 (admitted)
     */
    @KafkaListener(
            topics = "notification.queue",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeQueueEvent(
            @Payload EventTemplate eventTemplate,
            @Header(KafkaHeaders.RECEIVED_KEY) String userId,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        consumeEvent(topic, partition, offset, userId, eventTemplate, acknowledgment);
    }

    /**
     * notification.auction 토픽 리스너
     * - 상회 입찰 알림 (outbid)
     * - 경매 종료 알림 (closed)
     */
    @KafkaListener(
            topics = "notification.auction",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAuctionEvent(
            @Payload EventTemplate eventTemplate,
            @Header(KafkaHeaders.RECEIVED_KEY) String userId,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        consumeEvent(topic, partition, offset, userId, eventTemplate, acknowledgment);
    }

    /**
     * notification.order 토픽 리스너
     * - 결제 필요 알림 (payment_required)
     * - 결제 완료 알림 (paid)
     */
    @KafkaListener(
            topics = "notification.order",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEvent(
            @Payload EventTemplate eventTemplate,
            @Header(KafkaHeaders.RECEIVED_KEY) String userId,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        consumeEvent(topic, partition, offset, userId, eventTemplate, acknowledgment);
    }

    /**
     * 공통 이벤트 처리 메서드
     * - 리스너 역할: 최소한의 검증만 수행
     * - 성공한 경우에만 ack, 실패 시 예외를 던져서 DLT/ErrorHandler가 처리하도록 함
     */
    private void consumeEvent(
            String topic,
            int partition,
            long offset,
            String userId,
            EventTemplate eventTemplate,
            Acknowledgment acknowledgment
    ) {
        // null-safe 로그
        log.info("Kafka 이벤트 수신: topic={}, partition={}, offset={}, eventId={}, eventType={}, userId={}",
                topic, partition, offset,
                eventTemplate != null ? eventTemplate.eventId() : null,
                eventTemplate != null ? eventTemplate.eventType() : null,
                userId);

        // eventTemplate null 방어 (NPE 방지)
        if (eventTemplate == null) {
            log.error("payload가 null입니다. topic={}, partition={}, offset={}, userId={}",
                    topic, partition, offset, userId);
            throw new IllegalArgumentException("payload는 필수입니다");
        }

        // eventType null/blank 체크
        String eventType = eventTemplate.eventType();
        if (eventType == null || eventType.isBlank()) {
            log.error("eventType이 null이거나 비어있습니다. eventId={}, topic={}",
                    eventTemplate.eventId(), topic);
            throw new IllegalArgumentException("eventType은 필수입니다");
        }

        // 나머지는 service에 맡김
        // 성공한 경우에만 ack, 실패 시 예외를 던져서 DLT/ErrorHandler가 처리하도록 함
        try {
            eventNotificationService.processEvent(eventTemplate, topic);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("이벤트 처리 실패: topic={}, partition={}, offset={}, eventId={}, error={}",
                    topic, partition, offset, eventTemplate.eventId(), e.getMessage(), e);
            throw e; // 예외를 던져서 DLT/ErrorHandler가 처리하도록 함
        }
    }

}

