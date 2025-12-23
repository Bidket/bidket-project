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
public class OrderCanceledEventProcessor {

    private final PaymentTimeoutSagaOrchestrator sagaOrchestrator;

    @Transactional
    public void processOrderCanceled(Map<String, Object> payload) {
        log.info("[OrderCanceledEventProcessor] ORDER_CANCELED 이벤트 처리 시작: payload={}", payload);

        try {
             
            Map<String, Object> data = getDataMap(payload);

            UUID orderId = extractUuid(data, "orderId");
            UUID auctionId = extractUuid(data, "auctionId");
            UUID userId = extractUuid(data, "userId");
            String reason = extractString(data, "reason");

            if (orderId == null || auctionId == null) {
                log.error("[OrderCanceledEventProcessor] 필수 정보 누락: orderId={}, auctionId={}",
                        orderId, auctionId);
                return;
            }

            log.info("[OrderCanceledEventProcessor] 주문 취소로 인한 경매 재오픈 처리: orderId={}, auctionId={}, userId={}, reason={}",
                    orderId, auctionId, userId, reason);

            UUID sagaId = sagaOrchestrator.startPaymentTimeoutSaga(auctionId, orderId);

            log.info("[OrderCanceledEventProcessor] ORDER_CANCELED 이벤트 처리 완료: sagaId={}, auctionId={}, orderId={}",
                    sagaId, auctionId, orderId);

        } catch (Exception e) {
            log.error("[OrderCanceledEventProcessor] ORDER_CANCELED 이벤트 처리 실패: error={}",
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
            log.warn("[OrderCanceledEventProcessor] UUID 파싱 실패: key={}, value={}", key, value);
            return null;
        }
    }

    private String extractString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
}
