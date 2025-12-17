package com.bidket.auction.application.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Order Service 이벤트를 로깅만 하는 기본 핸들러
 * 실제 Saga 구현체가 없을 때 사용되는 Mock 구현체
 *
 * SAGA-001 (경매 종료 Saga) 구현 시, 실제 구현체로 교체해야 합니다.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(OrderSagaMessageHandler.class)
public class LoggingOrderSagaMessageHandler implements OrderSagaMessageHandler {

    @Override
    public void handleOrderCreated(Map<String, Object> payload) {
        log.info("[LoggingOrderSagaMessageHandler] ORDER_CREATED 수신: payload={}", payload);

        // TODO: SAGA-001 구현 시, 실제 Saga 로직으로 교체
        // 1. Saga 상태를 ORDER_CREATED로 업데이트
        // 2. orderId를 Saga 컨텍스트에 저장
        // 3. 다음 Saga 단계 (MARK_WINNING_BID) 실행
    }

    @Override
    public void handleOrderCreationFailed(Map<String, Object> payload) {
        log.warn("[LoggingOrderSagaMessageHandler] ORDER_CREATION_FAILED 수신: payload={}", payload);

        // TODO: SAGA-001 구현 시, 실제 보상 트랜잭션 로직으로 교체
        // 1. Saga 상태를 FAILED로 업데이트
        // 2. 실패 사유 기록
        // 3. retryable이 true이면 재시도 여부 판단
        // 4. retryable이 false이면 경매 재오픈 (AUC-010)
    }
}
