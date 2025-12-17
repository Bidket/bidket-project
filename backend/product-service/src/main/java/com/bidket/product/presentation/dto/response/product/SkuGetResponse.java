package com.bidket.product.presentation.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "sku 조회 응답")
public record SkuGetResponse(

        @Schema(description = "sku ID", example = "31885914-120f-4b55-b891-5f0d92d12f09")
        UUID id,

        @Schema(description = "상품 ID", example = "d4750ee2-97ff-40b6-a1c1-f056cd41c5fd")
        UUID productId,

        @Schema(description = "사이즈 ID", example = "806f7b28-67f1-4ee0-b41f-1ab7696eb249")
        UUID sizeId,

        @Schema(description = "sku 코드", example = "AM97-KR260(상품모델코드 + 사이즈)")
        String skuCode,

        @Schema(description = "sku 상태", example = "ACTIVE, INACTIVE")
        String status
) {}
