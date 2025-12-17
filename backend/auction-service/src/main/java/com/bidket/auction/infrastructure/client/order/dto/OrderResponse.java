package com.bidket.auction.infrastructure.client.order.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Order Service 응답 DTO
 * INT-003: Order-service 연동
 */
@Data
public class OrderResponse {
    private UUID orderId;
    private UUID auctionId;
    private UUID userId;
    private String status;  // PENDING_PAYMENT, PAID, CANCELLED, etc.
    private Long amount;
    private LocalDateTime paymentDeadline;
    private LocalDateTime createdAt;
}
