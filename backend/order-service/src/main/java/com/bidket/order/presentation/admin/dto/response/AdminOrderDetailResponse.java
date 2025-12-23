package com.bidket.order.presentation.admin.dto.response;

import com.bidket.order.application.admin.info.AdminOrderInfo;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminOrderDetailResponse(
        UUID orderId,
        UUID userId,
        UUID auctionId,
        UUID shoeId,
        String status,
        Long amount,
        LocalDateTime createdAt
) {

    public static AdminOrderDetailResponse from(AdminOrderInfo info) {
        return new AdminOrderDetailResponse(
                info.orderId(),
                info.userId(),
                info.auctionId(),
                info.shoeId(),
                info.status().name(),
                info.amount(),
                info.createdAt()
        );
    }
}