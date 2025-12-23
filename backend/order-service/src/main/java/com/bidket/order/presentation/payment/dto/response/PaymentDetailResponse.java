package com.bidket.order.presentation.payment.dto.response;

import com.bidket.order.domain.payment.model.Payment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "결제 상세 조회 응답")
public record PaymentDetailResponse(

        @Schema(example = "44ae4803-fbf7-446c-9632-fcccdd2840ab")
        UUID paymentId,

        @Schema(example = "614c565d-f9de-4261-b518-203ad424fd31")
        UUID orderId,

        @Schema(example = "CARD")
        String method,

        @Schema(example = "100000")
        Long amount,

        @Schema(example = "0")
        Long usedPointAmount,

        @Schema(example = "SUCCESS")
        String status,

        LocalDateTime createdAt,
        
        LocalDateTime updatedAt
) {

    public static PaymentDetailResponse from(Payment payment) {
        return new PaymentDetailResponse(
                payment.id(),
                payment.orderId(),
                payment.method().name(),
                payment.amount(),
                payment.usedPointAmount(),
                payment.status().name(),
                payment.createdAt(),
                payment.updatedAt()
        );
    }
}