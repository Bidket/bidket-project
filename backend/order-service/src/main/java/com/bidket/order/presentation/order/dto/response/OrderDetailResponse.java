package com.bidket.order.presentation.order.dto.response;

import com.bidket.order.application.order.info.OrderInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "주문 상세 조회 응답")
public record OrderDetailResponse(

        @Schema(description = "주문 ID", example = "a1b2c3d4-5678-90ab-cdef-1234567890ab")
        String orderId,

        @Schema(description = "주문 상태", example = "PAID")
        String status,

        @Schema(description = "주문 금액", example = "80000")
        Long amount,

        @Schema(description = "사용 포인트 금액", example = "10000")
        Long usedPointAmount,

        @Schema(description = "결제 만료 시간", example = "2025-12-01T12:00:00")
        LocalDateTime paymentExpiredAt,

        @Schema(description = "경매 ID", example = "11111111-2222-3333-4444-555555555555")
        String auctionId,

        @Schema(description = "상품(사이즈) ID", example = "ac25ba50-c3f6-4556-85ef-f64ec4262cfa")
        String shoeId,

        @Schema(description = "생성일시", example = "2025-11-20T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정일시", example = "2025-11-20T10:05:00")
        LocalDateTime updatedAt
) {

    public static OrderDetailResponse from(OrderInfo info) {
        return new OrderDetailResponse(
                info.getOrderId().toString(),
                info.getStatus().name(),
                info.getAmount(),
                info.getUsedPointAmount(),
                info.getPaymentExpiredAt(),
                info.getAuctionId().toString(),
                info.getShoeId().toString(),
                info.getCreatedAt(),
                info.getUpdatedAt()
        );
    }
}