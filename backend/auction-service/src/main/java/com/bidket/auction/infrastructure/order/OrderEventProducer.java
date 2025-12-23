package com.bidket.auction.infrastructure.order;

import com.bidket.auction.application.outbox.service.OutboxService;
import com.bidket.auction.infrastructure.kafka.event.CancelOrderRequestedEvent;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

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

        StandardEvent event = CreateOrderRequestedEvent.create(
                sagaId, auctionId, winnerUserId, productSizeId, price, correlationId
        );

        Map<String, Object> payload = convertToMap(event);

        AuctionOutbox outbox = outboxService.saveOrderEvent(
                event.eventType(),
                sagaId,  
                payload,
                UUID.fromString(event.data().get("correlationId").toString())
        );

        log.info("주문 생성 요청 OutBox 저장 완료: outboxId={}, sagaId={}", outbox.getId(), sagaId);
        return outbox;
    }

    @Transactional
    public AuctionOutbox publishCancelOrderRequest(
            UUID orderId,
            UUID auctionId,
            String reason,
            UUID correlationId
    ) {
        log.info("주문 취소 요청 이벤트 발행: orderId={}, auctionId={}, reason={}",
                orderId, auctionId, reason);

        StandardEvent event = CancelOrderRequestedEvent.create(
                orderId,
                auctionId,
                reason,
                correlationId
        );

        Map<String, Object> payload = convertToMap(event);

        AuctionOutbox outbox = outboxService.saveOrderEvent(
                event.eventType(),
                orderId,
                payload,
                correlationId
        );

        log.info("주문 취소 요청 OutBox 저장 완료: outboxId={}, orderId={}", outbox.getId(), orderId);
        return outbox;
    }

    private Map<String, Object> convertToMap(StandardEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventId", event.eventId().toString());
        map.put("occurredAt", event.occurredAt().toString());
        map.put("source", event.source());
        map.put("eventType", event.eventType());
        map.put("data", event.data());
        return map;
    }
}
