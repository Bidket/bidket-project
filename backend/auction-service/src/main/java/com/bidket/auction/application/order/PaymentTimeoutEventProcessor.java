package com.bidket.auction.application.order;

import com.bidket.auction.application.saga.PaymentTimeoutSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTimeoutEventProcessor {

    private final PaymentTimeoutSagaOrchestrator sagaOrchestrator;

    @Transactional
    public void processPaymentTimeout(Map<String, Object> payload) {
        log.info("[PaymentTimeoutEventProcessor] PAYMENT_TIMEOUT 이벤트 처리 시작: payload={}", payload);

        try {
             
            UUID orderId = extractUuid(payload, "orderId");
            UUID auctionId = extractUuid(payload, "auctionId");

            if (orderId == null || auctionId == null) {
                log.error("[PaymentTimeoutEventProcessor] 필수 정보 누락: orderId={}, auctionId={}",
                        orderId, auctionId);
                return;
            }

            UUID sagaId = sagaOrchestrator.startPaymentTimeoutSaga(auctionId, orderId);

            log.info("[PaymentTimeoutEventProcessor] PAYMENT_TIMEOUT 이벤트 처리 완료: sagaId={}, auctionId={}, orderId={}",
                    sagaId, auctionId, orderId);

        } catch (Exception e) {
            log.error("[PaymentTimeoutEventProcessor] PAYMENT_TIMEOUT 이벤트 처리 실패: error={}",
                    e.getMessage(), e);
            throw e;
        }
    }

    private UUID extractUuid(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            log.warn("[PaymentTimeoutEventProcessor] UUID 파싱 실패: key={}, value={}", key, value);
            return null;
        }
    }
}
