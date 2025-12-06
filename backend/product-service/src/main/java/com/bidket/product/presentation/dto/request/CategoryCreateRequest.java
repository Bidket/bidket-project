package com.bidket.product.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CategoryCreateRequest(

        @NotNull(message = "상품 타입 ID는 필수입니다.")
        UUID productTypeId,

        UUID parentId, // null → depth=0 취급됨

        @NotBlank(message = "카테고리 이름은 필수입니다.")
        @Pattern(regexp = "^[가-힣\\s]+$", message = "공백과 한글만 입력가능합니다.")
        @Size(max = 50, message = "카테고리 이름은 최대 50자입니다.")
        String name,

        @NotBlank(message = "카테고리 코드는 필수입니다.")
        @Pattern(regexp = "[A-Z_]+", message = "영어 대문자와 특수 문자 '_'만 입력가능합니다.")
        @Size(max = 100, message = "카테고리 코드는 최대 100자입니다.")
        String code,

        @NotNull(message = "정렬 값(sortId)은 필수입니다.")
        Long sortId
) {}
