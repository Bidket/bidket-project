package com.bidket.auction.application.compensation.actions;

import com.bidket.auction.application.compensation.CompensationAction;
import com.bidket.auction.infrastructure.order.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 주문 취소 보상 액션
 * Order Service와 연동하여 주문 취소 요청
 *
 * 사용 시나리오:
 * - AuctionEndSaga 실패 시 생성된 주문 취소
 * - 보상 트랜잭션의 일부로 실행
 */
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
            // Order Service에 주문 취소 요청 이벤트 발행
            orderEventProducer.publishCancelOrderRequest(
                    orderId,
                    null, // auctionId는 payload에서 추출 가능하지만 Order Service에서 조회 가능
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
