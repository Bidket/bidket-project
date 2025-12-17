package com.bidket.auction.infrastructure.kafka.consumer;

import com.bidket.auction.application.order.OrderEventProcessor;
import com.bidket.auction.application.processed.service.ProcessedEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Order Service로부터 발행되는 이벤트를 수신하는 Consumer
 * - ORDER_CREATED
 * - ORDER_CREATION_FAILED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderEventProcessor eventProcessor;
    private final ProcessedEventService processedEventService;

    @KafkaListener(
            topics = "${auction.kafka.topics.auction-order:auction.order}",
            containerFactory = "idempotentKafkaListenerContainerFactory"
    )
    public void handleOrderEvent(
            @Payload(required = false) Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = "eventId", required = false) String eventIdHeader,
            @Header(name = "eventType", required = false) String eventTypeHeader,
            @Header(name = "correlationId", required = false) String correlationIdHeader
    ) {
        // 역직렬화 실패로 payload가 null인 경우 스킵
        if (payload == null) {
            log.warn("[OrderEventConsumer] 역직렬화 실패로 메시지를 스킵합니다. topic={}", topic);
            return;
        }

        // 헤더가 없으면 payload에서 읽기 (fallback)
        UUID eventId = parseUuid(eventIdHeader);
        String eventType = StringUtils.defaultIfBlank(eventTypeHeader, extractString(payload, "eventType"));
        UUID correlationId = parseUuid(correlationIdHeader);

        // 헤더에 eventId가 없으면 payload에서 읽기
        if (eventId == null) {
            String eventIdFromPayload = extractString(payload, "eventId");
            eventId = parseUuid(eventIdFromPayload);
        }

        // 헤더에 correlationId가 없으면 payload에서 읽기
        if (correlationId == null) {
            String correlationIdFromPayload = extractString(payload, "correlationId");
            correlationId = parseUuid(correlationIdFromPayload);
        }

        // eventType이 여전히 없으면 UNKNOWN
        if (StringUtils.isBlank(eventType)) {
            eventType = "UNKNOWN";
        }

        if (eventId == null) {
            log.warn("[OrderEventConsumer] eventId가 없어 멱등성 체크를 건너뜁니다. topic={}, eventType={}", topic, eventType);
            eventProcessor.process(eventType, payload);
            return;
        }

        // 멱등성 체크
        log.info("[OrderEventConsumer] Order 이벤트 수신: eventId={}, eventType={}", eventId, eventType);
        boolean isProcessed = processedEventService.isProcessed(eventId);

        if (isProcessed) {
            log.info("[OrderEventConsumer] 중복 이벤트 감지로 스킵: eventId={}, eventType={}", eventId, eventType);
            return;
        }

        log.info("[OrderEventConsumer] 새 Order 이벤트 처리 시작: eventId={}, eventType={}", eventId, eventType);
        eventProcessor.process(eventType, payload);
        processedEventService.markProcessed(eventId, eventType, correlationId);
        log.info("[OrderEventConsumer] Order 이벤트 처리 완료: eventId={}", eventId);
    }

    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private UUID parseUuid(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            log.warn("[OrderEventConsumer] UUID 파싱 실패: {}", value);
            return null;
        }
    }
}
