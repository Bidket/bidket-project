package com.bidket.auction.application.order;

import java.util.Map;

/**
 * Order Service 이벤트를 Saga에 전달하는 인터페이스
 * AuctionEndSaga에서 이 인터페이스를 구현하여 주문 생성 결과를 받을 수 있습니다.
 *
 * 표준 이벤트 구조를 받습니다:
 * {
 *   "eventId": "...",
 *   "occurredAt": "...",
 *   "source": "order-service",
 *   "type": "ORDER_CREATED",
 *   "userId": "...",
 *   "data": { 핵심 정보 }
 * }
 */
public interface OrderSagaMessageHandler {

    /**
     * 주문 생성 성공 이벤트 처리
     *
     * @param payload ORDER_CREATED 표준 이벤트 (Map 형태)
     */
    void handleOrderCreated(Map<String, Object> payload);

    /**
     * 주문 생성 실패 이벤트 처리
     *
     * @param payload ORDER_CREATION_FAILED 표준 이벤트 (Map 형태)
     */
    void handleOrderCreationFailed(Map<String, Object> payload);
}
