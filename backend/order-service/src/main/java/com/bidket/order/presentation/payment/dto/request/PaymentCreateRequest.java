package com.bidket.order.presentation.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record PaymentCreateRequest(

        @Schema(description = "주문 ID", example = "a1b2c3d4-5678-90ab-cdef-1234567890ab")
        UUID orderId,

        @Schema(description = "결제 수단", example = "CARD")
        String method,

        @Schema(description = "결제 금액(원)", example = "50000")
        Long amount,

        @Schema(description = "사용 포인트 금액(원)", example = "10000")
        Long usePointAmount
) {

}