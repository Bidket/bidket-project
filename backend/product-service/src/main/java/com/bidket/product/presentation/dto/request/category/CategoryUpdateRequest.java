package com.bidket.product.presentation.dto.request.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "카테고리 수정 요청")
public record CategoryUpdateRequest(

        @Schema(description = "카테고리명", example = "러닝화")
        @Pattern(regexp = "^[가-힣\\s]+$", message = "공백과 한글만 입력가능합니다.")
        @Size(max = 50, message = "카테고리 이름은 최대 50자입니다.")
        String name,

        @Schema(description = "정렬 순서", example = "10")
        Long sortId
) {}
