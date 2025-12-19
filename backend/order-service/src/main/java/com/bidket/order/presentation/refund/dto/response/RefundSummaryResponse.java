package com.bidket.order.presentation.refund.dto.response;

import com.bidket.order.application.refund.info.RefundSummaryInfo;
import com.bidket.order.domain.refund.model.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "환불 목록 조회 응답(단일 아이템)")
public record RefundSummaryResponse(

        @Schema(description = "환불 ID", example = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        UUID refundId,

        @Schema(description = "결제 ID", example = "a1b2c3d4-5678-90ab-cdef-1234567890ab")
        UUID paymentId,

        @Schema(description = "환불 금액(원)", example = "50000")
        Long refundAmount,

        @Schema(description = "환불된 포인트 금액(원)", example = "10000")
        Long refundedPointAmount,

        @Schema(description = "환불 상태", example = "APPROVED")
        RefundStatus status,

        @Schema(description = "환불 승인 일시", example = "2025-11-26T11:10:00")
        LocalDateTime approvedAt
) {

    public static RefundSummaryResponse from(RefundSummaryInfo info) {
        return new RefundSummaryResponse(
                info.refundId(),
                info.paymentId(),
                info.refundAmount(),
                info.refundedPointAmount(),
                info.status(),
                info.approvedAt()
        );
    }
}