package com.bidket.auction.infrastructure.event.payment;

import com.bidket.auction.application.outbox.service.OutboxService;
import com.bidket.auction.infrastructure.kafka.event.StandardEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final OutboxService outboxService;

    public void publishCancelPaymentRequest(
            UUID paymentId,
            UUID orderId,
            UUID auctionId,
            UUID userId,
            String reason,
            UUID correlationId
    ) {
        log.info("[PaymentEventProducer] 결제 취소 요청 이벤트 발행 시작: paymentId={}, orderId={}, auctionId={}, userId={}, reason={}",
                paymentId, orderId, auctionId, userId, reason);

        try {
            StandardEvent event = CancelPaymentRequestedEvent.create(
                    paymentId,
                    orderId,
                    auctionId,
                    userId,
                    reason,
                    correlationId
            );

            Map<String, Object> payload = convertToMap(event);

            outboxService.savePaymentEvent(
                    event.eventType(),
                    paymentId,
                    payload,
                    correlationId
            );

            log.info("[PaymentEventProducer] 결제 취소 요청 이벤트 발행 완료: paymentId={}, eventId={}",
                    paymentId, event.eventId());

        } catch (Exception e) {
            log.error("[PaymentEventProducer] 결제 취소 요청 이벤트 발행 실패: paymentId={}, error={}",
                    paymentId, e.getMessage(), e);
            throw e;
        }
    }

    private Map<String, Object> convertToMap(StandardEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("eventId", event.eventId().toString());
        map.put("eventType", event.eventType());
        map.put("occurredAt", event.occurredAt().toString());
        map.put("source", event.source());
        map.put("userId", event.userId() != null ? event.userId().toString() : null);
        map.put("data", event.data());
        return map;
    }
}
