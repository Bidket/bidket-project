package com.bidket.auction.application.compensation.actions;

import com.bidket.auction.application.compensation.CompensationAction;
import com.bidket.auction.infrastructure.event.payment.PaymentEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 취소 보상 액션
 * Payment Service와 연동하여 결제 취소/환불 요청
 *
 * 사용 시나리오:
 * 1. AuctionEndSaga 실패 - 결제 완료 후 주문 생성 실패 시
 * 2. PaymentTimeoutSaga 실행 전 결제 완료된 경우
 * 3. 배송 실패로 인한 결제 환불
 * 4. 시스템 오류로 인한 트랜잭션 롤백
 *
 * Note: 결제가 완료되지 않은 상태에서는 실행되지 않음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CancelPaymentCompensationAction implements CompensationAction {

    private final PaymentEventProducer paymentEventProducer;

    @Override
    @Transactional
    public void execute(UUID sagaId, UUID paymentId, String payload) throws Exception {
        log.info("[CancelPaymentCompensation] 결제 취소 보상 시작: sagaId={}, paymentId={}",
                sagaId, paymentId);

        if (paymentId == null) {
            log.warn("[CancelPaymentCompensation] paymentId가 null이므로 Skip: sagaId={}", sagaId);
            return;
        }

        try {
            // Payment Service에 결제 취소 요청 이벤트 발행
            paymentEventProducer.publishCancelPaymentRequest(
                    paymentId,
                    null, // orderId - Payment Service에서 조회 가능
                    null, // auctionId - Payment Service에서 조회 가능
                    "SAGA_COMPENSATION",
                    sagaId
            );

            log.info("[CancelPaymentCompensation] 결제 취소 요청 이벤트 발행 완료: paymentId={}, sagaId={}",
                    paymentId, sagaId);

        } catch (Exception e) {
            log.error("[CancelPaymentCompensation] 결제 취소 보상 실패: paymentId={}, error={}",
                    paymentId, e.getMessage(), e);
            throw e;
        }
    }
}
