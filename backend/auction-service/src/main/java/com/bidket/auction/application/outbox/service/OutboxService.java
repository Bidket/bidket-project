package com.bidket.auction.application.outbox.service;

import com.bidket.auction.domain.outbox.model.AuctionOutbox;
import com.bidket.auction.domain.outbox.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final String AGGREGATE_TYPE_AUCTION = "AUCTION";
    private static final String AGGREGATE_TYPE_ORDER = "ORDER";
    private static final String AGGREGATE_TYPE_NOTIFICATION = "NOTIFICATION";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public AuctionOutbox saveAuctionEvent(String eventType,
                                          UUID aggregateId,
                                          Map<String, Object> payload,
                                          UUID correlationId) {
        return saveEvent(AGGREGATE_TYPE_AUCTION, eventType, aggregateId, payload, correlationId);
    }

    public AuctionOutbox saveOrderEvent(String eventType,
                                        UUID aggregateId,
                                        Map<String, Object> payload,
                                        UUID correlationId) {
        return saveEvent(AGGREGATE_TYPE_ORDER, eventType, aggregateId, payload, correlationId);
    }

    public AuctionOutbox saveNotificationEvent(String eventType,
                                               UUID userId,
                                               Map<String, Object> payload,
                                               UUID correlationId) {
        return saveEvent(AGGREGATE_TYPE_NOTIFICATION, eventType, userId, payload, correlationId);
    }

    private AuctionOutbox saveEvent(String aggregateType,
                                    String eventType,
                                    UUID aggregateId,
                                    Map<String, Object> payload,
                                    UUID correlationId) {
        try {
            log.info("Outbox 저장 시작: aggregateType={}, eventType={}, aggregateId={}, correlationId={}",
                    aggregateType, eventType, aggregateId, correlationId);
            String payloadJson = toJson(payload);
            AuctionOutbox outbox = AuctionOutbox.pending(
                    aggregateType,
                    aggregateId,
                    eventType,
                    payloadJson,
                    correlationId
            );

            AuctionOutbox saved = outboxRepository.save(outbox);
            log.info("Outbox 저장 완료: id={}, eventType={}, aggregateId={}", saved.getId(), eventType, aggregateId);
            return saved;
        } catch (Exception e) {
            log.error("Outbox 저장 실패: eventType={}, aggregateId={}, correlationId={}", eventType, aggregateId, correlationId, e);
            throw e;
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox payload 직렬화 실패", e);
        }
    }
}
