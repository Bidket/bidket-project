package com.bidket.order.presentation.order.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "주문 목록 조회 - 주문 요약 응답")
public record OrderSummaryResponse(

        @Schema(description = "주문 ID", example = "a1b2c3d4-5678-90ab-cdef-1234567890ab")
        String orderId,

        @Schema(description = "주문 상태", example = "PAID")
        String status,

        @Schema(description = "주문 결제 금액(원)", example = "80000")
        Long amount,

        @Schema(description = "신발 상품명", example = "Nike Air Jordan 1 Retro High OG")
        String productName,

        @Schema(description = "경매 제목", example = "조던 1 레트로 하이 한정판 경매 1차")
        String auctionTitle,

        @Schema(description = "경매 시작 시간", example = "2025-11-30T17:00:00")
        LocalDateTime auctionStartTime,

        @Schema(description = "주문 생성 시각", example = "2025-11-20T10:00:00")
        LocalDateTime createdAt
) {

}