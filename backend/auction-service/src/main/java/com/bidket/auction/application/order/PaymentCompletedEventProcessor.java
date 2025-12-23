package com.bidket.auction.application.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCompletedEventProcessor {

    @Transactional
    public void processPaymentCompleted(Map<String, Object> payload) {
        log.info("[PaymentCompletedEventProcessor] PAYMENT_COMPLETED 이벤트 처리 시작: payload={}", payload);

        try {
             
            Map<String, Object> data = getDataMap(payload);

            UUID orderId = extractUuid(data, "orderId");
            UUID paymentId = extractUuid(data, "paymentId");
            UUID auctionId = extractUuid(data, "auctionId");
            UUID userId = extractUuid(data, "userId");
            Long amount = extractLong(data, "amount");

            log.info("[PaymentCompletedEventProcessor] 결제 완료 기록: orderId={}, paymentId={}, auctionId={}, userId={}, amount={}",
                    orderId, paymentId, auctionId, userId, amount);

            log.info("[PaymentCompletedEventProcessor] PAYMENT_COMPLETED 이벤트 처리 완료: auctionId={}, orderId={}",
                    auctionId, orderId);

        } catch (Exception e) {
            log.error("[PaymentCompletedEventProcessor] PAYMENT_COMPLETED 이벤트 처리 실패: error={}",
                    e.getMessage(), e);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getDataMap(Map<String, Object> payload) {
        Object dataObj = payload.get("data");
        if (dataObj instanceof Map) {
            return (Map<String, Object>) dataObj;
        }
         
        return payload;
    }

    private UUID extractUuid(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            log.warn("[PaymentCompletedEventProcessor] UUID 파싱 실패: key={}, value={}", key, value);
            return null;
        }
    }

    private Long extractLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("[PaymentCompletedEventProcessor] Long 파싱 실패: key={}, value={}", key, value);
            return null;
        }
    }
}
