package com.bidket.auction.application.compensation.actions;

import com.bidket.auction.application.compensation.CompensationAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 주문 취소 보상 액션
 * Order Service와 연동하여 주문 취소 요청
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CancelOrderCompensationAction implements CompensationAction {

    // TODO: OrderEventProducer 주입 (확장 버전)
    // private final OrderEventProducer orderEventProducer;

    @Override
    @Transactional
    public void execute(UUID sagaId, UUID orderId, String payload) throws Exception {
        log.info("[CancelOrderCompensation] 주문 취소 시작: sagaId={}, orderId={}",
                sagaId, orderId);

        // TODO: Order Service에 주문 취소 요청 이벤트 발행
        // CancelOrderRequestedEvent event = CancelOrderRequestedEvent.create(
        //     sagaId, orderId, "SAGA_COMPENSATION"
        // );
        // orderEventProducer.publishCancelOrderRequest(event);

        // MVP: 로깅만 수행
        log.warn("[CancelOrderCompensation] 주문 취소 이벤트 발행 (MVP: 미구현): orderId={}",
                orderId);
    }
}
