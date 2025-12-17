package com.bidket.product.presentation.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "sku 목록 조회 요청")
public record SkuGetRequest(

        @Schema(description = "상품 ID", example = "d4750ee2-97ff-40b6-a1c1-f056cd41c5fd")
        UUID productId,

        @Schema(description = "사이즈 ID", example = "806f7b28-67f1-4ee0-b41f-1ab7696eb249")
        UUID sizeId,

        @Schema(description = "sku 상태", example = "ACTIVE, INACTIVE")
        String status
) {}
