package com.bidket.order.presentation.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 상태 조회 응답")
public record OrderStatusResponse(

        @Schema(description = "주문 ID", example = "a1b2c3d4-5678-90ab-cdef-1234567890ab")
        String orderId,

        @Schema(description = "주문 상태", example = "PAYMENT")
        String status
) {

}