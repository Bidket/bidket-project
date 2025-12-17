package com.bidket.product.presentation.dto.request.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "상품 검색 요청")
public record ProductSearchRequest(

        @Schema(description = "검색 키워드", example = "Air Force, AM-97, Nike, 나이키")
        String keyword, // 상품명, 모델명, 브랜드명 등 키워드

        @Schema(description = "상품 타입 ID", example = "8f3bf029-5761-4e33-8ab0-b3426871eb41")
        UUID productTypeId, // 상품 타입 필터 (SHOES)

        @Schema(description = "브랜드 ID", example = "8f3bf029-5761-4e33-8ab0-b3426871eb41")
        UUID brandId, // 브랜드 필터

        @Schema(description = "카테고리 Id", example = "8f3bf029-5761-4e33-8ab0-b3426871eb41")
        UUID categoryId, // 카테고리 필터(대표 카테고리)

        @Schema(description = "성별", example = "UNISEX, MEN, WOMEN, KIDS")
        String gender,

        @Schema(description = "최소 발매가(원)", example = "100000")
        BigDecimal minPrice,

        @Schema(description = "최대 발매가(원)", example = "300000")
        BigDecimal maxPrice
) {
    public String normalizedGender() {
        return gender == null || gender().isBlank()
                ? null : gender.toUpperCase();
    }
}
