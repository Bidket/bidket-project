package com.bidket.order.presentation.payment.dto.response;

import com.bidket.order.domain.payment.model.Payment;
import com.bidket.order.domain.payment.model.PaymentMethod;
import com.bidket.order.domain.payment.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "결제 요청 생성 응답")
public record PaymentCreateResponse(

        @Schema(description = "결제 ID", example = "a1b2c3d4-5678-90ab-cdef-1234567890ab")
        UUID paymentId,

        @Schema(description = "주문 ID", example = "11111111-2222-3333-4444-555555555555")
        UUID orderId,

        @Schema(description = "결제 수단", example = "CARD")
        PaymentMethod method,

        @Schema(description = "결제 금액(원)", example = "50000")
        Long amount,

        @Schema(description = "사용 포인트 금액(원)", example = "10000")
        Long usedPointAmount,

        @Schema(description = "결제 상태", example = "PENDING")
        PaymentStatus status,

        @Schema(description = "결제 요청 생성 시각", example = "2025-11-26T10:01:00")
        LocalDateTime createdAt

        // TODO: Payple 연동 후 paymentKey, pgRedirectUrl 필드 추가 예정
) {

    public static PaymentCreateResponse from(Payment payment) {
        return new PaymentCreateResponse(
                payment.id(),
                payment.orderId(),
                payment.method(),
                payment.amount(),
                payment.usedPointAmount(),
                payment.status(),
                payment.createdAt()
        );
    }
}