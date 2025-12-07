package com.bidket.order.presentation.dto.response;

import com.bidket.order.application.info.OrderInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import lombok.Getter;

@Getter
@Schema(description = "신발 경매 주문 생성 응답")
public class OrderCreateResponse {

    @Schema(description = "주문 ID", example = "a1b2c3d4-5678-90ab-cdef-1234567890ab")
    private final String orderId;

    @Schema(description = "회원 ID", example = "11111111-2222-3333-4444-555555555555")
    private final String userId;

    @Schema(description = "경매 ID", example = "7c4d3f9a-1b2c-4d5e-9f01-23456789abcd")
    private final String auctionId;

    @Schema(description = "신발 ID", example = "b1a2c3d4-e5f6-7890-abcd-ef0123456789")
    private final String shoeId;

    @Schema(description = "주문 상태", example = "PAYMENT")
    private final String status;

    @Schema(description = "주문 결제 금액(원)", example = "250000")
    private final Long amount;

    @Schema(description = "사용 포인트 금액(원)", example = "10000")
    private final Long usedPointAmount;

    @Schema(description = "결제 만료 시각", example = "2025-12-01T12:00:00")
    private final String paymentExpiredAt;

    @Schema(description = "주문 생성 시각", example = "2025-12-01T11:00:00")
    private final String createdAt;

    @Schema(description = "주문 수정 시각", example = "2025-12-01T11:00:00")
    private final String updatedAt;

    private OrderCreateResponse(
            String orderId,
            String userId,
            String auctionId,
            String shoeId,
            String status,
            Long amount,
            Long usedPointAmount,
            String paymentExpiredAt,
            String createdAt,
            String updatedAt
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.auctionId = auctionId;
        this.shoeId = shoeId;
        this.status = status;
        this.amount = amount;
        this.usedPointAmount = usedPointAmount;
        this.paymentExpiredAt = paymentExpiredAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OrderCreateResponse from(OrderInfo orderInfo) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        return new OrderCreateResponse(
                orderInfo.getOrderId().toString(),
                orderInfo.getUserId().toString(),
                orderInfo.getAuctionId().toString(),
                orderInfo.getShoeId().toString(),
                orderInfo.getStatus().name(),
                orderInfo.getAmount(),
                orderInfo.getUsedPointAmount(),
                orderInfo.getPaymentExpiredAt().format(formatter),
                orderInfo.getCreatedAt().format(formatter),
                orderInfo.getUpdatedAt().format(formatter)
        );
    }
}