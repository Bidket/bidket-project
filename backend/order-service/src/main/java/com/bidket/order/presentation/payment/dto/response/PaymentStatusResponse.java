package com.bidket.order.presentation.payment.dto.response;

import com.bidket.order.domain.payment.model.Payment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "결제 상태 응답")
public record PaymentStatusResponse(
        @Schema(description = "결제 ID")
        UUID paymentId,

        @Schema(description = "주문 ID")
        UUID orderId,

        @Schema(description = "결제 상태(SUCCESS/FAILED)")
        String status,

        @Schema(description = "처리 시각")
        LocalDateTime updatedAt
) {

    public static PaymentStatusResponse from(Payment payment) {
        return new PaymentStatusResponse(
                payment.id(),
                payment.orderId(),
                payment.status().name(),
                payment.updatedAt()
        );
    }
}