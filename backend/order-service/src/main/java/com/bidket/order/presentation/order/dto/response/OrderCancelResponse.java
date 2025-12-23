package com.bidket.order.presentation.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "주문 취소 응답")
public record OrderCancelResponse(

        @Schema(description = "주문 ID")
        String orderId,

        @Schema(description = "주문 상태", example = "CANCELED")
        String status,

        @Schema(description = "취소 시각")
        LocalDateTime canceledAt
) {

}