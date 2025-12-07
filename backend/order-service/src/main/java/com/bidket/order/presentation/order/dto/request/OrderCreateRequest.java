package com.bidket.order.presentation.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "신발 경매 주문 생성 요청")
public record OrderCreateRequest(

        @NotNull
        @Schema(description = "주문 요청 유저 ID", example = "11111111-2222-3333-4444-555555555555")
        String userId,

        @NotBlank
        @Schema(description = "경매 ID", example = "7c4d3f9a-1b2c-4d5e-9f01-23456789abcd")
        String auctionId,

        @NotBlank
        @Schema(description = "신발 ID", example = "b1a2c3d4-e5f6-7890-abcd-ef0123456789")
        String shoeId,

        @NotNull
        @Min(1)
        @Schema(description = "주문 결제 금액(원)", example = "250000")
        Long amount,

        @Min(0)
        @Schema(description = "사용 포인트 금액(원)", example = "10000")
        Long usePointAmount
) {

}