package com.bidket.order.presentation.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record PaymentFailRequest(
        @Schema(description = "주문 ID", example = "11111111-2222-3333-4444-555555555555")
        UUID orderId,

        @Schema(description = "PG 에러 코드", example = "PAYMENT_CANCELED")
        String errorCode,

        @Schema(description = "PG 에러 메시지", example = "사용자에 의해 결제가 취소되었습니다.")
        String errorMessage
) {

}