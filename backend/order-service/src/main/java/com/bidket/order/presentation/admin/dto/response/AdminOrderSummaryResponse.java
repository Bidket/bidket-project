package com.bidket.order.presentation.admin.dto.response;

import com.bidket.order.application.admin.info.AdminOrderSummaryInfo;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminOrderSummaryResponse(
        UUID orderId,
        UUID userId,
        String status,
        Long amount,
        LocalDateTime createdAt
) {

    public static AdminOrderSummaryResponse from(AdminOrderSummaryInfo info) {
        return new AdminOrderSummaryResponse(
                info.orderId(),
                info.userId(),
                info.status().name(),
                info.amount(),
                info.createdAt()
        );
    }
}