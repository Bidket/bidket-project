package com.bidket.auction.infrastructure.event.payment;

import com.bidket.auction.application.outbox.service.OutboxService;
import com.bidket.auction.infrastructure.kafka.event.StandardEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
            String reason,
            UUID correlationId
    ) {
        log.info("[PaymentEventProducer] 결제 취소 요청 이벤트 발행 시작: paymentId={}, orderId={}, auctionId={}, reason={}",
                paymentId, orderId, auctionId, reason);

        try {
            StandardEvent event = CancelPaymentRequestedEvent.create(
                    paymentId,
                    orderId,
                    auctionId,
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

    public void publishCancelPaymentRequestWithUser(
            UUID paymentId,
            UUID orderId,
            UUID auctionId,
            UUID userId,
            String reason,
            UUID correlationId
    ) {
        log.info("[PaymentEventProducer] 결제 취소 요청 이벤트 발행 시작 (사용자 포함): paymentId={}, userId={}, reason={}",
                paymentId, userId, reason);

        try {
            StandardEvent event = CancelPaymentRequestedEvent.createWithUser(
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

            log.info("[PaymentEventProducer] 결제 취소 요청 이벤트 발행 완료: paymentId={}, userId={}, eventId={}",
                    paymentId, userId, event.eventId());

        } catch (Exception e) {
            log.error("[PaymentEventProducer] 결제 취소 요청 이벤트 발행 실패: paymentId={}, userId={}, error={}",
                    paymentId, userId, e.getMessage(), e);
            throw e;
        }
    }

    private Map<String, Object> convertToMap(StandardEvent event) {
        return Map.of(
                "eventId", event.eventId().toString(),
                "occurredAt", event.occurredAt().toString(),
                "source", event.source(),
                "eventType", event.eventType(),
                "data", event.data()
        );
    }
}
