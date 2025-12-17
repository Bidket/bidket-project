package com.bidket.auction.application.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Order Service 이벤트를 처리하는 기본 구현체
 * 표준 이벤트 구조를 파싱하여 적절한 Saga 핸들러로 라우팅
 *
 * 표준 이벤트 구조:
 * {
 *   "eventId": "...",
 *   "occurredAt": "...",
 *   "source": "order-service",
 *   "type": "ORDER_CREATED",
 *   "userId": "...",
 *   "data": { ... } // 핵심 정보만
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultOrderEventProcessor implements OrderEventProcessor {

    private static final String EVENT_TYPE_ORDER_CREATED = "ORDER_CREATED";
    private static final String EVENT_TYPE_ORDER_CREATION_FAILED = "ORDER_CREATION_FAILED";
    private static final String EVENT_TYPE_PAYMENT_TIMEOUT = "PAYMENT_TIMEOUT";

    private final OrderSagaMessageHandler sagaMessageHandler;
    private final PaymentTimeoutEventProcessor paymentTimeoutEventProcessor;

    @Override
    public void process(String eventType, Map<String, Object> payload) {
        log.info("[OrderEventProcessor] 이벤트 처리 시작: eventType={}", eventType);

        switch (eventType) {
            case EVENT_TYPE_ORDER_CREATED:
                handleOrderCreated(payload);
                break;
            case EVENT_TYPE_ORDER_CREATION_FAILED:
                handleOrderCreationFailed(payload);
                break;
            case EVENT_TYPE_PAYMENT_TIMEOUT:
                handlePaymentTimeout(payload);
                break;
            default:
                log.warn("[OrderEventProcessor] 알 수 없는 이벤트 타입: {}", eventType);
        }
    }

    private void handleOrderCreated(Map<String, Object> payload) {
        try {
            // 표준 이벤트 구조에서 data 추출
            Map<String, Object> data = getDataMap(payload);

            UUID sagaId = parseUuid(data, "sagaId");
            UUID orderId = parseUuid(data, "orderId");
            UUID auctionId = parseUuid(data, "auctionId");

            log.info("[OrderEventProcessor] ORDER_CREATED 처리: sagaId={}, orderId={}, auctionId={}",
                    sagaId, orderId, auctionId);

            sagaMessageHandler.handleOrderCreated(payload);
        } catch (Exception e) {
            log.error("[OrderEventProcessor] ORDER_CREATED 처리 실패: payload={}", payload, e);
            throw new RuntimeException("ORDER_CREATED 이벤트 처리 실패", e);
        }
    }

    private void handleOrderCreationFailed(Map<String, Object> payload) {
        try {
            // 표준 이벤트 구조에서 data 추출
            Map<String, Object> data = getDataMap(payload);

            UUID sagaId = parseUuid(data, "sagaId");
            UUID auctionId = parseUuid(data, "auctionId");
            String failureReason = parseString(data, "failureReason");

            log.info("[OrderEventProcessor] ORDER_CREATION_FAILED 처리: sagaId={}, auctionId={}, reason={}",
                    sagaId, auctionId, failureReason);

            sagaMessageHandler.handleOrderCreationFailed(payload);
        } catch (Exception e) {
            log.error("[OrderEventProcessor] ORDER_CREATION_FAILED 처리 실패: payload={}", payload, e);
            throw new RuntimeException("ORDER_CREATION_FAILED 이벤트 처리 실패", e);
        }
    }

    private void handlePaymentTimeout(Map<String, Object> payload) {
        try {
            log.info("[OrderEventProcessor] PAYMENT_TIMEOUT 처리: orderId={}, auctionId={}",
                    payload.get("orderId"), payload.get("auctionId"));
            paymentTimeoutEventProcessor.processPaymentTimeout(payload);
        } catch (Exception e) {
            log.error("[OrderEventProcessor] PAYMENT_TIMEOUT 처리 실패: payload={}", payload, e);
            throw new RuntimeException("PAYMENT_TIMEOUT 이벤트 처리 실패", e);
        }
    }

    /**
     * 표준 이벤트 구조에서 data 필드를 추출합니다.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getDataMap(Map<String, Object> payload) {
        Object dataObj = payload.get("data");
        if (dataObj instanceof Map) {
            return (Map<String, Object>) dataObj;
        }
        throw new IllegalArgumentException("Invalid event structure: 'data' field is missing or not a Map");
    }

    private UUID parseUuid(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof UUID) {
            return (UUID) value;
        }
        return UUID.fromString(value.toString());
    }

    private Long parseLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String parseString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }

    private LocalDateTime parseLocalDateTime(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        return LocalDateTime.parse(value.toString());
    }

    private boolean parseBoolean(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
}
