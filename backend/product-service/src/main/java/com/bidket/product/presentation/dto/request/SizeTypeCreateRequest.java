package com.bidket.product.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SizeTypeCreateRequest(

        @NotNull(message = "상품 타입 ID는 필수입니다.")
        UUID productTypeId,

        @NotBlank(message = "사이즈 타입 코드 값은 필수입니다.")
        @Pattern(regexp = "[A-Z_]+$+", message = "영어 대문자와 특수 문자 '_'만 입력가능합니다.")
        @Size(max = 50, message = "코드는 최대 50자입니다.")
        String code,

        @Pattern(regexp = "^[A-Z]+$", message = "영어 대문자만 입력가능합니다.")
        @Size(max = 10, message = "지역 코드는 최대 10자입니다.")
        String regionCode,

        @Size(max = 500, message = "설명은 최대 500자입니다.")
        String description,

        Boolean isDefault // default 여부 (nullable → false 처리)
) {}
