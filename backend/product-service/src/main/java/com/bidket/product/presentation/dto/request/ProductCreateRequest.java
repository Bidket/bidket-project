package com.bidket.product.presentation.dto.request;

import com.bidket.product.domain.model.Gender;
import com.bidket.product.domain.model.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductCreateRequest(

        @NotNull(message = "상품 타입 ID는 필수입니다.")
        UUID productTypeId,

        @NotNull(message = "브랜드 ID는 필수입니다.")
        UUID brandId,

        @NotBlank(message = "상품 이름은 필수입니다.")
        @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "공백과 영어 대소문자만 입력가능합니다.")
        @Size(max = 100, message = "상품 이름은 최대 100자입니다.")
        String name,

        @Pattern(regexp = "^[가-힣\\s]+$", message = "공백과 한글만 입력가능합니다.")
        @Size(max = 100, message = "상품 한글명은 최대 100자입니다.")
        String nameKr,

        @NotBlank(message = "모델 코드 값은 필수입니다.")
        @Pattern(regexp = "[A-Z0-9]+$", message = "영어 대문자와 숫자만 입력가능합니다.")
        @Size(max = 100, message = "모델 코드는 최대 100자입니다.")
        String modelCode,

        Gender gender,

        @Size(max = 2000, message = "설명은 최대 2000자까지 입력 가능합니다.")
        String description,

        LocalDate releaseDate,

        @NotBlank(message = "발매가는 필수입니다.")
        @DecimalMin(value = "0.0", inclusive = true, message = "발매가는 0 이상이어야 합니다.")
        BigDecimal releasePrice,

        @NotNull(message = "상품 상태는 필수입니다.")
        ProductStatus status
) {}
