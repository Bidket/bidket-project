package com.bidket.product.presentation.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "상품 검색 응답")
public record ProductSearchResponse(

        @Schema(description = "상품 ID", example = "UUID")
        UUID id,

        @Schema(description = "상품이 속한 브랜드명", example = "Nike")
        String brandName,

        @Schema(description = "상품명", example = "Nike Dunk Low Retro Panda")
        String name,

        @Schema(description = "상품 한글명", example = "나이키 덩크 로우 레트로 팬더")
        String nameKr,

        @Schema(description = "상품 모델코드", example = "DD1391-100")
        String modelCode,

        @Schema(description = "상품 발매가(원)", example = "139000")
        BigDecimal releasePrice
) {}
