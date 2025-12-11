package com.bidket.order.presentation.refund.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "환불 요청 생성 요청")
public record RefundCreateRequest(

        @Schema(description = "환불 금액(원)", example = "50000")
        Long refundAmount,

        @Schema(description = "환불 포인트 금액(원)", example = "10000")
        Long refundPointAmount,

        @Schema(description = "환불 사유", example = "단순 변심")
        String reason
) {

}