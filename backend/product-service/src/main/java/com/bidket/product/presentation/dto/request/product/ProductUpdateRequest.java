package com.bidket.product.presentation.dto.request.product;

import com.bidket.product.domain.model.Gender;
import com.bidket.product.presentation.dto.request.shoesdetail.ProductShoesDetailUpdateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "상품과 상품 상세 수정 요청")
public record ProductUpdateRequest(

        // Product
        @Schema(description = "브랜드 ID", example = "UUID")
        UUID brandId,

        @Schema(description = "상품명", example = "Air Force 1")
        @Pattern(regexp = "^[a-zA-Z0-9\\s]+$", message = "공백과 영어 대소문자 숫자만 입력가능합니다.")
        @Size(max = 100, message = "상품 이름은 최대 100자입니다.")
        String name,

        @Schema(description = "상품 한글명", example = "에어 포스 1")
        @Pattern(regexp = "^[가-힣0-9\\s]+$", message = "공백과 한글 숫자만 입력가능합니다.")
        @Size(max = 100, message = "상품 한글명은 최대 100자입니다.")
        String nameKr,

        @Schema(description = "상품 모델코드", example = "CW2288-111")
        @Pattern(
                regexp = "[A-Z0-9_-]+$",
                message = "영어 대문자와 숫자 특수문자 '-','_' 만 입력가능합니다.")
        @Size(max = 100, message = "모델 코드는 최대 100자입니다.")
        String modelCode,

        @Schema(description = "상품 성별", example = "UNISEX")
        Gender gender,

        @Schema(description = "상품 설명", example = "에어포스1 설명")
        @Size(max = 2000, message = "설명은 최대 2000자까지 입력 가능합니다.")
        String description,

        @Schema(description = "상품 발매일", example = "2025-12-22", pattern = "yyyy-MM-dd")
        LocalDate releaseDate,

        @Schema(description = "상품 발매가", example = "190000")
        @DecimalMin(value = "0.0", inclusive = true, message = "발매가는 0 이상이어야 합니다.")
        BigDecimal releasePrice,

        // Category
        List<UUID> categoryIds,
        UUID primaryCategoryId,

        // Detail (Shoes)
        ProductShoesDetailUpdateRequest shoesDetail
) {}
