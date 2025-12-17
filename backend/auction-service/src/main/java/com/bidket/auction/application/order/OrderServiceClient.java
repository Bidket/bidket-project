package com.bidket.auction.application.order;

import com.bidket.auction.infrastructure.client.order.OrderClient;
import com.bidket.auction.infrastructure.client.order.dto.OrderResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Order Service 클라이언트 서비스
 * INT-003: Order-service 연동
 *
 * 기존 OrderClient(Feign)를 주입받아 사용
 * Circuit Breaker 및 Retry 패턴 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceClient {

    private final OrderClient orderClient;

    /**
     * 경매 ID로 주문 조회
     *
     * Circuit Breaker: orderService (설정 파일 참조)
     * Retry: sagaStep (설정 파일 참조)
     *
     * @param auctionId 경매 ID
     * @return 주문 정보 (Optional)
     */
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrderByAuctionIdFallback")
    @Retry(name = "sagaStep")
    public Optional<OrderResponse> getOrderByAuctionId(UUID auctionId) {
        log.info("[OrderServiceClient] 주문 조회 시작: auctionId={}", auctionId);

        try {
            OrderResponse response = orderClient.getOrderByAuctionId(auctionId);
            log.info("[OrderServiceClient] 주문 조회 성공: auctionId={}, orderId={}",
                    auctionId, response.getOrderId());
            return Optional.ofNullable(response);

        } catch (Exception e) {
            log.error("[OrderServiceClient] 주문 조회 실패: auctionId={}, error={}",
                    auctionId, e.getMessage());
            throw e;
        }
    }

    /**
     * Circuit Breaker Fallback 메서드
     *
     * @param auctionId 경매 ID
     * @param e 발생한 예외
     * @return 빈 Optional
     */
    private Optional<OrderResponse> getOrderByAuctionIdFallback(UUID auctionId, Exception e) {
        log.warn("[OrderServiceClient] Circuit Breaker 활성화 또는 재시도 실패: auctionId={}, error={}",
                auctionId, e.getMessage());
        return Optional.empty();
    }
}
