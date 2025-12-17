package com.bidket.product.presentation.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "상품 상세 조회 응답")
public record ProductPageGetResponse(

        @Schema(description = "상품 ID", example = "UUID")
        UUID id,

        @Schema(description = "상품명", example = "Nike Dunk Low Retro Panda")
        String name,

        @Schema(description = "상품 한글명", example = "나이키 덩크 로우 레트로 팬더")
        String nameKr,

        @Schema(description = "상품 모델코드", example = "DD1391-100")
        String modelCode,

        @Schema(description = "성별", example = "UNISEX, MEN, WOMEN, KIDS")
        String gender,

        @Schema(description = "상품 발매가(원)", example = "139000")
        BigDecimal releasePrice,

        @Schema(description = "상품이 속한 브랜드명", example = "Nike")
        String brandName,

        @Schema(description = "상품 타입명", example = "신발")
        String productTypeName,

        @Schema(description = "상품이 속한 카테고리 목록")
        List<CategoryInfo> categories,

        @Schema(description = "상품에 속한 sku 목록")
        List<SkuGetResponse> skus,

        @Schema(description = "상품 상세 정보(신발)")
        ShoesDetailInfo shoesDetail
) {
    public record CategoryInfo(
            UUID id,
            String name,
            int depth
    ) {}

    public record ShoesDetailInfo(
            String colorway,
            String mainMaterial,
            String silhouette,
            String style,
            String originCountry,
            BigDecimal weight
    ) {}
}
