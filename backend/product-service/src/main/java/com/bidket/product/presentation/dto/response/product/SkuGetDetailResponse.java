package com.bidket.product.presentation.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "sku 상세 조회 응답")
public record SkuGetDetailResponse(

        //Sku 정보
        @Schema(description = "sku ID", example = "31885914-120f-4b55-b891-5f0d92d12f09")
        UUID id,

        @Schema(description = "sku 코드", example = "AM97-KR260(상품모델코드 + 사이즈)")
        String skuCode,

        @Schema(description = "sku 상태", example = "ACTIVE, INACTIVE")
        String skuStatus,

        // Size 정보
        @Schema(description = "사이즈 ID", example = "806f7b28-67f1-4ee0-b41f-1ab7696eb249")
        UUID sizeId,

        @Schema(description = "사이즈 값", example = "260")
        String sizeLabel,

        // Product 정보
        @Schema(description = "상품 ID", example = "d4750ee2-97ff-40b6-a1c1-f056cd41c5fd")
        UUID productId,

        @Schema(description = "상품명", example = "Nike Dunk Low Retro Panda")
        String productName,

        @Schema(description = "브랜드명", example = "Nike")
        String brandName,

        @Schema(description = "상품 모델 코드", example = "DD1391-100")
        String modelCode,

        @Schema(description = "성별", example = "UNISEX, MEN, WOMEN, KIDS")
        String gender,

        @Schema(description = "상품 발매가(원)", example = "190000")
        BigDecimal releasePrice
) {}
