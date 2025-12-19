package com.bidket.auction.infrastructure.event.payment;

import com.bidket.auction.application.outbox.service.OutboxService;
import com.bidket.auction.infrastructure.kafka.event.StandardEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Payment 관련 이벤트 발행
 * OutBox 패턴을 사용하여 트랜잭션 안정성 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final OutboxService outboxService;

    /**
     * 결제 취소 요청 이벤트 발행
     * Payment Service로 결제 취소/환불 요청
     *
     * 사용 시나리오:
     * - Saga 보상 트랜잭션에서 결제 취소 필요 시
     * - 결제 완료 후 배송 실패 시
     * - 사용자가 결제 완료 후 주문 취소 시
     *
     * @param paymentId 취소할 결제 ID
     * @param orderId 관련 주문 ID
     * @param auctionId 관련 경매 ID
     * @param reason 취소 사유 (예: SAGA_COMPENSATION, DELIVERY_FAILURE, USER_CANCELLATION)
     * @param correlationId 분산 트레이싱용 상관관계 ID
     */
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

    /**
     * 사용자 ID 포함하는 결제 취소 요청 이벤트 발행
     */
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
                "userId", event.userId() != null ? event.userId() : "",
                "data", event.data()
        );
    }
}
