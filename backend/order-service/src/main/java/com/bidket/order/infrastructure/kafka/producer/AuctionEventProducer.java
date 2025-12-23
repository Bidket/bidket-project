package com.bidket.order.infrastructure.kafka.producer;

import com.bidket.order.application.outbox.service.OutboxService;
import com.bidket.order.infrastructure.kafka.event.OrderCanceledEvent;
import com.bidket.order.infrastructure.kafka.event.OrderCreatedEvent;
import com.bidket.order.infrastructure.kafka.event.OrderCreationFailedEvent;
import com.bidket.order.infrastructure.kafka.event.PaymentCompletedEvent;
import com.bidket.order.infrastructure.kafka.event.PaymentTimeoutEvent;
import com.bidket.order.infrastructure.kafka.event.StandardEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Auction Service로 이벤트를 발행하는 Producer
 * OutBox 패턴 적용: 이벤트를 OutBox 테이블에 저장하고, 스케줄러가 Kafka로 발행
 * - ORDER_CREATED: 주문 생성 완료
 * - ORDER_CREATION_FAILED: 주문 생성 실패
 * - PAYMENT_COMPLETED: 결제 완료
 * - PAYMENT_TIMEOUT: 결제 시간 초과
 * - ORDER_CANCELED: 주문 취소
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionEventProducer {

    private final OutboxService outboxService;

    /**
     * ORDER_CREATED 이벤트 발행 (OutBox 패턴)
     * Auction Service에 주문 생성 완료를 알립니다.
     */
    public void publishOrderCreated(
            UUID orderId,
            UUID sagaId,
            UUID auctionId,
            UUID userId,
            UUID correlationId
    ) {
        StandardEvent event = OrderCreatedEvent.create(
                orderId,
                sagaId,
                auctionId,
                userId,
                correlationId
        );

        Map<String, Object> payload = buildMessage(event);

        outboxService.saveOrderEvent(
                "ORDER_CREATED",
                orderId,
                payload,
                correlationId
        );

        log.info("[AuctionEventProducer] ORDER_CREATED 이벤트 OutBox 저장 완료: orderId={}, sagaId={}",
                orderId, sagaId);
    }

    /**
     * ORDER_CREATION_FAILED 이벤트 발행 (OutBox 패턴)
     * Auction Service에 주문 생성 실패를 알립니다.
     */
    public void publishOrderCreationFailed(
            UUID sagaId,
            UUID auctionId,
            UUID userId,
            String reason,
            UUID correlationId
    ) {
        StandardEvent event = OrderCreationFailedEvent.create(
                sagaId,
                auctionId,
                userId,
                reason,
                correlationId
        );

        Map<String, Object> payload = buildMessage(event);

        outboxService.saveOrderEvent(
                "ORDER_CREATION_FAILED",
                auctionId,
                payload,
                correlationId
        );

        log.info("[AuctionEventProducer] ORDER_CREATION_FAILED 이벤트 OutBox 저장 완료: sagaId={}, reason={}",
                sagaId, reason);
    }

    /**
     * PAYMENT_COMPLETED 이벤트 발행 (OutBox 패턴)
     * Auction Service에 결제 완료를 알립니다.
     */
    public void publishPaymentCompleted(
            UUID orderId,
            UUID paymentId,
            UUID auctionId,
            UUID userId,
            Long amount,
            UUID correlationId
    ) {
        StandardEvent event = PaymentCompletedEvent.create(
                orderId,
                paymentId,
                auctionId,
                userId,
                amount,
                correlationId
        );

        Map<String, Object> payload = buildMessage(event);

        outboxService.savePaymentEvent(
                "PAYMENT_COMPLETED",
                paymentId,
                payload,
                correlationId
        );

        log.info("[AuctionEventProducer] PAYMENT_COMPLETED 이벤트 OutBox 저장 완료: orderId={}, paymentId={}, amount={}",
                orderId, paymentId, amount);
    }

    /**
     * PAYMENT_TIMEOUT 이벤트 발행 (OutBox 패턴)
     * Auction Service에 결제 시간 초과를 알립니다.
     */
    public void publishPaymentTimeout(
            UUID orderId,
            UUID auctionId,
            UUID userId,
            String reason,
            UUID correlationId
    ) {
        StandardEvent event = PaymentTimeoutEvent.create(
                orderId,
                auctionId,
                userId,
                reason,
                correlationId
        );

        Map<String, Object> payload = buildMessage(event);

        outboxService.saveOrderEvent(
                "PAYMENT_TIMEOUT",
                orderId,
                payload,
                correlationId
        );

        log.info("[AuctionEventProducer] PAYMENT_TIMEOUT 이벤트 OutBox 저장 완료: orderId={}, reason={}",
                orderId, reason);
    }

    /**
     * ORDER_CANCELED 이벤트 발행 (OutBox 패턴)
     * Auction Service에 주문 취소를 알립니다.
     */
    public void publishOrderCanceled(
            UUID orderId,
            UUID auctionId,
            UUID userId,
            String reason,
            UUID correlationId
    ) {
        StandardEvent event = OrderCanceledEvent.create(
                orderId,
                auctionId,
                userId,
                reason,
                correlationId
        );

        Map<String, Object> payload = buildMessage(event);

        outboxService.saveOrderEvent(
                "ORDER_CANCELED",
                orderId,
                payload,
                correlationId
        );

        log.info("[AuctionEventProducer] ORDER_CANCELED 이벤트 OutBox 저장 완료: orderId={}, reason={}",
                orderId, reason);
    }

    /**
     * StandardEvent를 Kafka 메시지 형식으로 변환
     */
    private Map<String, Object> buildMessage(StandardEvent event) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("eventId", event.eventId().toString());
        message.put("occurredAt", event.occurredAt().toString());
        message.put("source", event.source());
        message.put("eventType", event.eventType());
        message.put("data", event.data());
        return message;
    }
}
