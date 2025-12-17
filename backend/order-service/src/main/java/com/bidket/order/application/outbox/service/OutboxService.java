package com.bidket.order.application.outbox.service;

import com.bidket.order.domain.outbox.model.OrderOutbox;
import com.bidket.order.domain.outbox.repository.OrderOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * OutBox Service
 *
 * Transactional Outbox Pattern 구현
 * - 이벤트를 OutBox 테이블에 저장
 * - 비즈니스 트랜잭션과 동일한 트랜잭션 내에서 실행
 * - 이벤트 발행은 OutboxPublisher가 별도로 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final String AGGREGATE_TYPE_ORDER = "ORDER";
    private static final String AGGREGATE_TYPE_PAYMENT = "PAYMENT";
    private static final String AGGREGATE_TYPE_REFUND = "REFUND";

    private final OrderOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * 주문 관련 이벤트를 OutBox에 저장
     *
     * @param eventType 이벤트 타입 (ORDER_CREATED, ORDER_CANCELED 등)
     * @param aggregateId 주문 ID
     * @param payload 이벤트 데이터
     * @param correlationId Saga 추적 ID
     * @return 저장된 OrderOutbox
     */
    @Transactional
    public OrderOutbox saveOrderEvent(
            String eventType,
            UUID aggregateId,
            Map<String, Object> payload,
            UUID correlationId
    ) {
        return saveEvent(AGGREGATE_TYPE_ORDER, eventType, aggregateId, payload, correlationId);
    }

    /**
     * 결제 관련 이벤트를 OutBox에 저장
     *
     * @param eventType 이벤트 타입 (PAYMENT_COMPLETED, PAYMENT_FAILED 등)
     * @param aggregateId 결제 ID
     * @param payload 이벤트 데이터
     * @param correlationId Saga 추적 ID
     * @return 저장된 OrderOutbox
     */
    @Transactional
    public OrderOutbox savePaymentEvent(
            String eventType,
            UUID aggregateId,
            Map<String, Object> payload,
            UUID correlationId
    ) {
        return saveEvent(AGGREGATE_TYPE_PAYMENT, eventType, aggregateId, payload, correlationId);
    }

    /**
     * 환불 관련 이벤트를 OutBox에 저장
     *
     * @param eventType 이벤트 타입 (REFUND_APPROVED, REFUND_REJECTED 등)
     * @param aggregateId 환불 ID
     * @param payload 이벤트 데이터
     * @param correlationId Saga 추적 ID
     * @return 저장된 OrderOutbox
     */
    @Transactional
    public OrderOutbox saveRefundEvent(
            String eventType,
            UUID aggregateId,
            Map<String, Object> payload,
            UUID correlationId
    ) {
        return saveEvent(AGGREGATE_TYPE_REFUND, eventType, aggregateId, payload, correlationId);
    }

    /**
     * 이벤트를 OutBox에 저장하는 공통 메서드
     */
    private OrderOutbox saveEvent(
            String aggregateType,
            String eventType,
            UUID aggregateId,
            Map<String, Object> payload,
            UUID correlationId
    ) {
        try {
            log.info("[OutboxService] OutBox 저장 시작: aggregateType={}, eventType={}, aggregateId={}, correlationId={}",
                    aggregateType, eventType, aggregateId, correlationId);

            String payloadJson = toJson(payload);
            LocalDateTime now = LocalDateTime.now();

            OrderOutbox outbox = OrderOutbox.pending(
                    aggregateType,
                    aggregateId,
                    eventType,
                    payloadJson,
                    correlationId,
                    now
            );

            OrderOutbox saved = outboxRepository.save(outbox);

            log.info("[OutboxService] OutBox 저장 완료: id={}, eventType={}, aggregateId={}",
                    saved.id(), eventType, aggregateId);

            return saved;

        } catch (Exception e) {
            log.error("[OutboxService] OutBox 저장 실패: eventType={}, aggregateId={}, correlationId={}",
                    eventType, aggregateId, correlationId, e);
            throw new RuntimeException("OutBox 저장 실패", e);
        }
    }

    /**
     * Map을 JSON 문자열로 직렬화
     */
    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("[OutboxService] Payload JSON 직렬화 실패: payload={}", payload, e);
            throw new IllegalStateException("OutBox payload 직렬화 실패", e);
        }
    }
}
