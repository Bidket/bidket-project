package com.bidket.order.presentation.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record PaymentConfirmRequest(
        @Schema(description = "PG 결제 키", example = "pg-generated-key")
        String paymentKey,

        @Schema(description = "주문 ID", example = "11111111-2222-3333-4444-555555555555")
        UUID orderId,

        @Schema(description = "결제 금액(원)", example = "50000")
        Long amount
) {

}