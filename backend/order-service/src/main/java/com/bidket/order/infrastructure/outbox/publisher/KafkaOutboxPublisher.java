package com.bidket.order.infrastructure.outbox.publisher;

import com.bidket.order.application.outbox.publisher.OutboxEventPublisher;
import com.bidket.order.domain.outbox.model.OrderOutbox;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka를 통한 OutBox 이벤트 발행 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOutboxPublisher implements OutboxEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${order.kafka.topics.auction-order:auction.order}")
    private String auctionOrderTopic;

    @Override
    public void publish(OrderOutbox outbox) {
        String topic = resolveTopic(outbox.aggregateType());
        Map<String, Object> message = buildEnvelope(outbox);

        try {
            ProducerRecord<String, Object> record = new ProducerRecord<>(
                    topic,
                    outbox.aggregateId().toString(), // 파티션 키
                    message
            );

            // 헤더에 메타데이터 추가
            record.headers().add("eventId", outbox.id().toString().getBytes());
            record.headers().add("eventType", outbox.eventType().getBytes());
            if (outbox.correlationId() != null) {
                record.headers().add("correlationId", outbox.correlationId().toString().getBytes());
            }

            kafkaTemplate.send(record).get();
            log.debug("[KafkaOutboxPublisher] Kafka 발행 성공: id={}, topic={}, eventType={}",
                    outbox.id(), topic, outbox.eventType());

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka publish interrupted", ie);
        } catch (Exception e) {
            throw new RuntimeException("Kafka publish failed", e);
        }
    }

    /**
     * OutBox를 Kafka 메시지 형식으로 변환
     */
    private Map<String, Object> buildEnvelope(OrderOutbox outbox) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("eventId", outbox.id());
        envelope.put("eventType", outbox.eventType());
        envelope.put("aggregateType", outbox.aggregateType());
        envelope.put("aggregateId", outbox.aggregateId());
        envelope.put("correlationId", outbox.correlationId());
        envelope.put("occurredAt", LocalDateTime.now(clock));
        envelope.put("payload", readPayload(outbox.payload()));
        return envelope;
    }

    /**
     * JSON payload를 Map으로 역직렬화
     */
    private Map<String, Object> readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Outbox payload 역직렬화 실패", e);
        }
    }

    /**
     * aggregateType에 따라 적절한 토픽을 결정
     *
     * - ORDER: Order → Auction 이벤트 → auction.order
     * - PAYMENT: Payment → Auction 이벤트 → auction.order
     * - REFUND: Refund → Auction 이벤트 → auction.order
     */
    private String resolveTopic(String aggregateType) {
        return switch (aggregateType.toUpperCase()) {
            case "ORDER", "PAYMENT", "REFUND" -> auctionOrderTopic;
            default -> auctionOrderTopic;
        };
    }
}
