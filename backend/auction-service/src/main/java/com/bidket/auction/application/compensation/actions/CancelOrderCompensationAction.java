package com.bidket.auction.application.compensation.actions;

import com.bidket.auction.application.compensation.CompensationAction;
import com.bidket.auction.infrastructure.order.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancelOrderCompensationAction implements CompensationAction {

    private final OrderEventProducer orderEventProducer;

    @Override
    @Transactional
    public void execute(UUID sagaId, UUID orderId, String payload) throws Exception {
        log.info("[CancelOrderCompensation] 주문 취소 보상 시작: sagaId={}, orderId={}",
                sagaId, orderId);

        if (orderId == null) {
            log.warn("[CancelOrderCompensation] orderId가 null이므로 Skip: sagaId={}", sagaId);
            return;
        }

        try {
             
            orderEventProducer.publishCancelOrderRequest(
                    orderId,
                    null,  
                    "SAGA_COMPENSATION",
                    sagaId
            );

            log.info("[CancelOrderCompensation] 주문 취소 요청 이벤트 발행 완료: orderId={}, sagaId={}",
                    orderId, sagaId);

        } catch (Exception e) {
            log.error("[CancelOrderCompensation] 주문 취소 보상 실패: orderId={}, error={}",
                    orderId, e.getMessage(), e);
            throw e;
        }
    }
}
