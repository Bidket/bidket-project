package com.bidket.order.presentation.refund.dto.response;

import com.bidket.order.domain.refund.model.Refund;
import com.bidket.order.domain.refund.model.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "환불 요청 생성 응답")
public record RefundCreateResponse(

        @Schema(description = "환불 ID", example = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        UUID refundId,

        @Schema(description = "결제 ID", example = "a1b2c3d4-5678-90ab-cdef-1234567890ab")
        UUID paymentId,

        @Schema(description = "환불 금액(원)", example = "50000")
        Long refundAmount,

        @Schema(description = "환불된 포인트 금액(원)", example = "10000")
        Long refundedPointAmount,

        @Schema(description = "환불 상태", example = "REQUESTED")
        RefundStatus status,

        @Schema(description = "환불 요청 일시", example = "2025-11-26T11:00:00")
        LocalDateTime requestedAt
) {

    public static RefundCreateResponse from(Refund refund) {
        return new RefundCreateResponse(
                refund.id(),
                refund.paymentId(),
                refund.refundAmount(),
                refund.refundedPointAmount(),
                refund.status(),
                refund.requestedAt()
        );
    }
}