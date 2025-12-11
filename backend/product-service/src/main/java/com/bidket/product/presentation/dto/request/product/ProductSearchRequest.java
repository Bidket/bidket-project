package com.bidket.product.presentation.dto.request.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSearchRequest(

        String keyword, // 상품명, 모델명, 브랜드명 등 키워드

        UUID productTypeId, // 상품 타입 필터 (SHOES)
        UUID brandId, // 브랜드 필터
        UUID categoryId, // 카테고리 필터(대표 카테고리)

        String gender, // "MEN", "WOMEN" 등

        BigDecimal minPrice, // 최소 발매가
        BigDecimal maxPrice // 최대 발매가
) {
    public String normalizedGender() {
        return gender == null || gender().isBlank()
                ? null : gender.toUpperCase();
    }
}
