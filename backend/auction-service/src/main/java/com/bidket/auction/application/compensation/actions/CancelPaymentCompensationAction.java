package com.bidket.auction.application.compensation.actions;

import com.bidket.auction.application.compensation.CompensationAction;
import com.bidket.auction.infrastructure.event.payment.PaymentEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

            paymentEventProducer.publishCancelPaymentRequest(
                    paymentId,
                    null,
                    null,
                    null,
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
