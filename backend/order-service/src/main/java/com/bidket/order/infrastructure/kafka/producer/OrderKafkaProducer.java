package com.bidket.order.infrastructure.kafka.producer;

import com.bidket.order.application.order.publisher.OrderEventPublisher;
import com.bidket.order.domain.outbox.OrderOutbox;
import com.bidket.order.domain.outbox.OrderOutboxRepository;
import com.bidket.order.infrastructure.kafka.event.OrderCreatedEvent;
import com.bidket.order.infrastructure.kafka.event.OrderCreationFailedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OrderEventPublisher 구현체
 * OutBox 패턴을 사용하여 이벤트를 OutBox 테이블에 저장
 * DDD: Infrastructure Layer - 이벤트 발행 메커니즘 구현
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaProducer implements OrderEventPublisher {

    private static final String EVENT_TYPE_ORDER_CREATED = "ORDER_CREATED";
    private static final String EVENT_TYPE_ORDER_CREATION_FAILED = "ORDER_CREATION_FAILED";

    private final OrderOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publishOrderCreated(
            UUID sagaId,
            UUID orderId,
            UUID auctionId,
            UUID userId,
            UUID productSizeId,
            Long amount,
            LocalDateTime paymentDeadline,
            LocalDateTime createdAt
    ) {
        try {
            log.info("[OrderKafkaProducer] ORDER_CREATED 이벤트 OutBox 저장: sagaId={}, orderId={}",
                    sagaId, orderId);

            // 1. Event 객체 생성
            OrderCreatedEvent event = new OrderCreatedEvent(
                    sagaId,
                    orderId,
                    auctionId,
                    userId,
                    productSizeId,
                    amount,
                    paymentDeadline,
                    createdAt
            );

            // 2. JSON 변환
            String payload = objectMapper.writeValueAsString(event);

            // 3. OutBox에 저장 (트랜잭션 커밋 후 폴링 Publisher가 발행)
            OrderOutbox outbox = OrderOutbox.pending(
                    sagaId,
                    EVENT_TYPE_ORDER_CREATED,
                    payload,
                    LocalDateTime.now()
            );

            outboxRepository.save(outbox);
            log.info("[OrderKafkaProducer] ORDER_CREATED OutBox 저장 완료: outboxId={}", outbox.id());

        } catch (JsonProcessingException e) {
            log.error("[OrderKafkaProducer] ORDER_CREATED JSON 변환 실패: sagaId={}", sagaId, e);
            throw new RuntimeException("Failed to serialize ORDER_CREATED event", e);
        }
    }

    @Override
    @Transactional
    public void publishOrderCreationFailed(
            UUID sagaId,
            UUID auctionId,
            UUID userId,
            String failureReason,
            String failureCode,
            LocalDateTime failedAt
    ) {
        try {
            log.info("[OrderKafkaProducer] ORDER_CREATION_FAILED 이벤트 OutBox 저장: sagaId={}, reason={}",
                    sagaId, failureReason);

            // 1. Event 객체 생성
            OrderCreationFailedEvent event = new OrderCreationFailedEvent(
                    sagaId,
                    auctionId,
                    userId,
                    failureReason,
                    failureCode,
                    failedAt
            );

            // 2. JSON 변환
            String payload = objectMapper.writeValueAsString(event);

            // 3. OutBox에 저장
            OrderOutbox outbox = OrderOutbox.pending(
                    sagaId,
                    EVENT_TYPE_ORDER_CREATION_FAILED,
                    payload,
                    LocalDateTime.now()
            );

            outboxRepository.save(outbox);
            log.info("[OrderKafkaProducer] ORDER_CREATION_FAILED OutBox 저장 완료: outboxId={}", outbox.id());

        } catch (JsonProcessingException e) {
            log.error("[OrderKafkaProducer] ORDER_CREATION_FAILED JSON 변환 실패: sagaId={}", sagaId, e);
            throw new RuntimeException("Failed to serialize ORDER_CREATION_FAILED event", e);
        }
    }
}
