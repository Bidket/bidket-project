package com.bidket.auction.infrastructure.client.order;

import com.bidket.auction.infrastructure.client.order.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Order Service Feign Client
 * INT-003: Order-service 연동
 *
 * Circuit Breaker: orderService (application.yml)
 * URL: ${feign.order-service.url}
 */
@FeignClient(
    name = "order-service",
    url = "${feign.order-service.url}"
)
public interface OrderClient {

    /**
     * 경매 ID로 주문 조회
     *
     * @param auctionId 경매 ID
     * @return 주문 정보
     */
    @GetMapping("/api/v1/orders/auction/{auctionId}")
    OrderResponse getOrderByAuctionId(@PathVariable("auctionId") UUID auctionId);
}
