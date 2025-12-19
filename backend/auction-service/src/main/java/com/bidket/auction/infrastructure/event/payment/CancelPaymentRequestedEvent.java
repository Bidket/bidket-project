package com.bidket.auction.infrastructure.event.payment;

import com.bidket.auction.infrastructure.kafka.event.StandardEvent;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 결제 취소 요청 이벤트
 * Payment Service로 결제 취소/환불을 요청
 *
 * 사용 시나리오:
 * 1. 결제 완료 후 사용자가 주문 취소
 * 2. 배송 실패로 인한 결제 취소
 * 3. 시스템 오류로 결제 후 트랜잭션 롤백 필요
 * 4. Saga 보상 트랜잭션에서 결제 취소 필요
 *
 * Note: Auction Service는 취소 요청만 발행
 * 실제 환불 처리는 Payment Service에서 수행
 */
public record CancelPaymentRequestedEvent() {

    /**
     * 결제 취소 요청 이벤트 생성
     *
     * @param paymentId 취소할 결제 ID
     * @param orderId 관련 주문 ID
     * @param auctionId 관련 경매 ID
     * @param reason 취소 사유 (예: DELIVERY_FAILURE, USER_CANCELLATION, SYSTEM_ERROR, SAGA_COMPENSATION)
     * @param correlationId 분산 트레이싱용 상관관계 ID
     * @return StandardEvent 형식의 이벤트
     */
    public static StandardEvent create(
            UUID paymentId,
            UUID orderId,
            UUID auctionId,
            String reason,
            UUID correlationId
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentId", paymentId.toString());
        data.put("orderId", orderId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("reason", reason);
        data.put("correlationId", correlationId.toString());
        data.put("requestedAt", LocalDateTime.now().toString());

        return new StandardEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                "auction-service",
                "CANCEL_PAYMENT_REQUESTED",
                null,  // userId는 context에 따라 나중에 추가 가능
                data
        );
    }

    /**
     * 사용자 ID 포함하는 결제 취소 요청 이벤트 생성
     */
    public static StandardEvent createWithUser(
            UUID paymentId,
            UUID orderId,
            UUID auctionId,
            UUID userId,
            String reason,
            UUID correlationId
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentId", paymentId.toString());
        data.put("orderId", orderId.toString());
        data.put("auctionId", auctionId.toString());
        data.put("reason", reason);
        data.put("correlationId", correlationId.toString());
        data.put("requestedAt", LocalDateTime.now().toString());

        return new StandardEvent(
                UUID.randomUUID(),
                LocalDateTime.now(),
                "auction-service",
                "CANCEL_PAYMENT_REQUESTED",
                userId,
                data
        );
    }
}
