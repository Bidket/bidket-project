package com.bidket.auction.infrastructure.outbox.publisher;

import com.bidket.auction.application.outbox.publisher.OutboxEventPublisher;
import com.bidket.auction.domain.outbox.model.AuctionOutbox;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.bidket.auction.infrastructure.kafka.config.KafkaTopics;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOutboxEventPublisher implements OutboxEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public void publish(AuctionOutbox outbox) {
        String topic = resolveTopic(outbox.getAggregateType());
        Map<String, Object> message = buildEnvelope(outbox);

        try {
            ProducerRecord<String, Object> record = new ProducerRecord<>(  
                    topic,  
                    outbox.getAggregateId().toString(),  
                    message  
            );
            
             
            record.headers().add("eventId", outbox.getId().toString().getBytes());
            record.headers().add("eventType", outbox.getEventType().getBytes());
            if (outbox.getCorrelationId() != null) {
                record.headers().add("correlationId", outbox.getCorrelationId().toString().getBytes());
            }
            
            kafkaTemplate.send(record).get();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka publish interrupted", ie);
        } catch (Exception e) {
            throw new RuntimeException("Kafka publish failed", e);
        }

        log.debug("Kafka 발행 성공: id={}, topic={}, eventType={}", outbox.getId(), topic, outbox.getEventType());
    }

    private Map<String, Object> buildEnvelope(AuctionOutbox outbox) {
         
        if ("ORDER".equalsIgnoreCase(outbox.getAggregateType())) {
            Map<String, Object> payload = readPayload(outbox.getPayload());
             
            return payload;
        }

        Map<String, Object> envelope = new HashMap<>();
        envelope.put("eventId", outbox.getId());
        envelope.put("eventType", outbox.getEventType());
        envelope.put("aggregateType", outbox.getAggregateType());
        envelope.put("aggregateId", outbox.getAggregateId());
        envelope.put("correlationId", outbox.getCorrelationId());
        envelope.put("occurredAt", LocalDateTime.now(clock));
        envelope.put("payload", readPayload(outbox.getPayload()));
        return envelope;
    }

    private Map<String, Object> readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Outbox payload 역직렬화 실패", e);
        }
    }

    private String resolveTopic(String aggregateType) {
        return switch (aggregateType.toUpperCase()) {
            case "ORDER", "SAGA" -> KafkaTopics.ORDER_AUCTION;
            case "AUCTION" -> KafkaTopics.AUCTION_EVENTS;
            case "NOTIFICATION" -> KafkaTopics.NOTIFICATION_AUCTION;
            default -> KafkaTopics.AUCTION_EVENTS;
        };
    }
}
