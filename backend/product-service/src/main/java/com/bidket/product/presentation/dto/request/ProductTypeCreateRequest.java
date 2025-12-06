package com.bidket.product.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductTypeCreateRequest(

        @NotBlank(message = "상품 타입 코드는 필수입니다.")
        @Pattern(regexp = "^[A-Z_]+$", message = "영어 대문자와 특수 문자 '_'만 입력가능합니다.")
        @Size(max = 50, message = "상품 타입 코드는 최대 50자입니다.")
        String code,

        @NotBlank(message = "상품 타입 이름은 필수입니다.")
        @Pattern(regexp = "^[가-힣]+$", message = "한글만 입력가능합니다.")
        @Size(max = 50, message = "상품 타입 이름은 최대 50자입니다.")
        String name,

        @Size(max = 500, message = "설명은 최대 500자까지 입력 가능합니다.")
        String description
) {}
