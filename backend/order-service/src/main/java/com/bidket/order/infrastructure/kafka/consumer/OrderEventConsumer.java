package com.bidket.order.infrastructure.kafka.consumer;

import com.bidket.order.application.order.facade.OrderFacade;
import com.bidket.order.application.processed.service.ProcessedEventService;
import com.bidket.order.infrastructure.kafka.producer.AuctionEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Auction Service로부터 발행되는 주문 이벤트를 수신하는 Consumer
 * - CREATE_ORDER_REQUESTED: 주문 생성 요청
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderFacade orderFacade;
    private final AuctionEventProducer auctionEventProducer;
    private final ProcessedEventService processedEventService;

    @KafkaListener(
            topics = "${order.kafka.topics.order-auction:order.auction}",
            containerFactory = "idempotentKafkaListenerContainerFactory"
    )
    public void handleAuctionEvent(
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
            processEvent(eventType, payload);
            return;
        }

        // 멱등성 체크
        log.info("[OrderEventConsumer] Auction 이벤트 수신: eventId={}, eventType={}", eventId, eventType);
        boolean isProcessed = processedEventService.isProcessed(eventId);

        if (isProcessed) {
            log.info("[OrderEventConsumer] 중복 이벤트 감지로 스킵: eventId={}, eventType={}", eventId, eventType);
            return;
        }

        log.info("[OrderEventConsumer] 새 Auction 이벤트 처리 시작: eventId={}, eventType={}", eventId, eventType);
        processEvent(eventType, payload);
        processedEventService.markProcessed(eventId, eventType, correlationId);
        log.info("[OrderEventConsumer] Auction 이벤트 처리 완료: eventId={}", eventId);
    }

    /**
     * 이벤트 타입별 처리 로직
     */
    private void processEvent(String eventType, Map<String, Object> payload) {
        switch (eventType) {
            case "CREATE_ORDER_REQUESTED" -> handleCreateOrderRequested(payload);
            default -> log.warn("[OrderEventConsumer] 알 수 없는 이벤트 타입: {}", eventType);
        }
    }

    /**
     * CREATE_ORDER_REQUESTED 이벤트 처리
     * - 주문 생성 요청을 받아 주문을 생성합니다.
     * - 성공 시 ORDER_CREATED 이벤트 발행
     * - 실패 시 ORDER_CREATION_FAILED 이벤트 발행
     */
    private void handleCreateOrderRequested(Map<String, Object> payload) {
        try {
            // payload에서 필수 데이터 추출
            UUID userId = parseUuid(extractString(payload, "userId"));
            Map<String, Object> data = extractMap(payload, "data");

            UUID sagaId = parseUuid(extractString(data, "sagaId"));
            UUID auctionId = parseUuid(extractString(data, "auctionId"));
            UUID productSizeId = parseUuid(extractString(data, "productSizeId"));
            Long price = extractLong(data, "price");
            String paymentDeadlineStr = extractString(data, "paymentDeadline");
            UUID correlationId = parseUuid(extractString(data, "correlationId"));

            // 필수 값 검증
            if (userId == null || sagaId == null || auctionId == null || productSizeId == null || price == null || correlationId == null) {
                log.error("[OrderEventConsumer] CREATE_ORDER_REQUESTED 이벤트 필수 데이터 누락: payload={}", payload);
                auctionEventProducer.publishOrderCreationFailed(
                        sagaId != null ? sagaId : UUID.randomUUID(),
                        auctionId != null ? auctionId : UUID.randomUUID(),
                        userId != null ? userId : UUID.randomUUID(),
                        "필수 데이터 누락",
                        correlationId != null ? correlationId : UUID.randomUUID()
                );
                return;
            }

            // paymentDeadline 파싱
            LocalDateTime paymentDeadline = null;
            if (paymentDeadlineStr != null) {
                try {
                    paymentDeadline = LocalDateTime.parse(paymentDeadlineStr);
                } catch (Exception e) {
                    log.warn("[OrderEventConsumer] paymentDeadline 파싱 실패, 기본값 사용: {}", paymentDeadlineStr);
                    paymentDeadline = LocalDateTime.now().plusMinutes(30);
                }
            } else {
                paymentDeadline = LocalDateTime.now().plusMinutes(30);
            }

            log.info("[OrderEventConsumer] 주문 생성 시작: userId={}, auctionId={}, productSizeId={}, price={}, sagaId={}",
                    userId, auctionId, productSizeId, price, sagaId);

            // 주문 생성 (OrderFacade 호출)
            var orderInfo = orderFacade.createOrderFromAuction(
                    userId,
                    auctionId,
                    productSizeId,
                    price,
                    0L, // usedPointAmount - 현재는 0으로 설정
                    paymentDeadline,
                    sagaId,
                    correlationId
            );

            log.info("[OrderEventConsumer] 주문 생성 완료: orderId={}, sagaId={}", orderInfo.getOrderId(), sagaId);

            // ORDER_CREATED 이벤트 발행
            auctionEventProducer.publishOrderCreated(
                    orderInfo.getOrderId(),
                    sagaId,
                    auctionId,
                    userId,
                    correlationId
            );

        } catch (Exception e) {
            log.error("[OrderEventConsumer] CREATE_ORDER_REQUESTED 처리 중 오류 발생", e);

            // 실패 이벤트 발행
            try {
                Map<String, Object> data = extractMap(payload, "data");
                UUID sagaId = parseUuid(extractString(data, "sagaId"));
                UUID auctionId = parseUuid(extractString(data, "auctionId"));
                UUID userId = parseUuid(extractString(payload, "userId"));
                UUID correlationId = parseUuid(extractString(data, "correlationId"));

                auctionEventProducer.publishOrderCreationFailed(
                        sagaId != null ? sagaId : UUID.randomUUID(),
                        auctionId != null ? auctionId : UUID.randomUUID(),
                        userId != null ? userId : UUID.randomUUID(),
                        e.getMessage(),
                        correlationId != null ? correlationId : UUID.randomUUID()
                );
            } catch (Exception publishError) {
                log.error("[OrderEventConsumer] ORDER_CREATION_FAILED 이벤트 발행 실패", publishError);
            }
        }
    }

    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new java.util.HashMap<>();
    }

    private Long extractLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("[OrderEventConsumer] Long 파싱 실패: key={}, value={}", key, value);
            return null;
        }
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
