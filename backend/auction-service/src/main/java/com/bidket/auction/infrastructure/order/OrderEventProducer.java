package com.bidket.auction.infrastructure.order;

import com.bidket.auction.application.outbox.service.OutboxService;
import com.bidket.auction.infrastructure.kafka.event.CreateOrderRequestedEvent;
import com.bidket.auction.infrastructure.kafka.event.StandardEvent;
import com.bidket.auction.domain.outbox.model.AuctionOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Order Service 연동을 위한 이벤트 Producer
 * OutBox 패턴을 사용하여 트랜잭션 커밋 후 이벤트 발행 보장
 *
 * 표준 이벤트 구조 사용:
 * - eventId, occurredAt, source, type, userId는 표준 필드
 * - data에는 핵심 정보만 포함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    /**
     * 주문 생성 요청 이벤트를 OutBox에 저장
     *
     * @param sagaId Saga ID
     * @param auctionId 경매 ID
     * @param winnerUserId 낙찰자 ID
     * @param productSizeId 상품 사이즈 ID
     * @param price 낙찰 금액
     * @param correlationId 상관 ID
     * @return 저장된 OutBox 엔티티
     */
    @Transactional
    public AuctionOutbox publishCreateOrderRequest(
            UUID sagaId,
            UUID auctionId,
            UUID winnerUserId,
            UUID productSizeId,
            Long price,
            UUID correlationId
    ) {
        log.info("주문 생성 요청 이벤트 발행: sagaId={}, auctionId={}, winnerUserId={}",
                sagaId, auctionId, winnerUserId);

        // 표준 이벤트 생성
        StandardEvent event = CreateOrderRequestedEvent.create(
                sagaId, auctionId, winnerUserId, productSizeId, price, correlationId
        );

        // StandardEvent를 Map으로 변환하여 OutBox에 저장
        Map<String, Object> payload = convertToMap(event);

        AuctionOutbox outbox = outboxService.saveOrderEvent(
                event.eventType(),
                sagaId, // aggregateId로 sagaId 사용
                payload,
                UUID.fromString(event.data().get("correlationId").toString())
        );

        log.info("주문 생성 요청 OutBox 저장 완료: outboxId={}, sagaId={}", outbox.getId(), sagaId);
        return outbox;
    }

    private Map<String, Object> convertToMap(StandardEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventId", event.eventId().toString());
        map.put("occurredAt", event.occurredAt().toString());
        map.put("source", event.source());
        map.put("eventType", event.eventType());
        map.put("userId", event.userId() != null ? event.userId().toString() : null);
        map.put("data", event.data());
        return map;
    }
}
