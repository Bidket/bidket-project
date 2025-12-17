package com.bidket.auction.application.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * PAYMENT_COMPLETED 이벤트 처리자
 * Order Service로부터 결제 완료 이벤트를 수신하여 처리
 *
 * 비즈니스 로직:
 * - 결제 완료 시 경매는 이미 SUCCESS 상태이므로 추가 상태 변경 없음
 * - 로깅 및 모니터링 목적으로 이벤트 기록
 * - 향후 확장: 판매자에게 알림, 정산 처리 등
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCompletedEventProcessor {

    /**
     * PAYMENT_COMPLETED 이벤트 처리
     *
     * @param payload 이벤트 페이로드
     */
    @Transactional
    public void processPaymentCompleted(Map<String, Object> payload) {
        log.info("[PaymentCompletedEventProcessor] PAYMENT_COMPLETED 이벤트 처리 시작: payload={}", payload);

        try {
            // 페이로드에서 정보 추출
            Map<String, Object> data = getDataMap(payload);

            UUID orderId = extractUuid(data, "orderId");
            UUID paymentId = extractUuid(data, "paymentId");
            UUID auctionId = extractUuid(data, "auctionId");
            UUID userId = extractUuid(data, "userId");
            Long amount = extractLong(data, "amount");

            log.info("[PaymentCompletedEventProcessor] 결제 완료 기록: orderId={}, paymentId={}, auctionId={}, userId={}, amount={}",
                    orderId, paymentId, auctionId, userId, amount);

            // TODO: 향후 확장 사항
            // 1. 판매자에게 결제 완료 알림
            // 2. 정산 시스템에 결제 정보 전달
            // 3. 경매 이력에 결제 완료 기록
            // 4. 비즈니스 메트릭 수집 (결제 성공률, 평균 결제 시간 등)

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
        // data 필드가 없으면 payload 자체를 사용 (fallback)
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
