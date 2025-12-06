package com.bidket.product.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SizeCreateRequest(

        @NotNull(message = "사이즈 타입 ID는 필수입니다.")
        UUID sizeTypeId,

        @NotBlank(message = "사이즈 코드 값은 필수입니다.")
        @Pattern(regexp = "[A-Z0-9]+$", message = "영어 대문자와 숫자만 입력가능합니다.")
        @Size(max = 50, message = "사이즈 코드는 최대 50자입니다.")
        String code,

        @NotBlank(message = "화면 표시용 사이즈 값은 필수입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9\\s.-]+$", message = "영어 대소문자와 숫자만 입력가능합니다.")
        @Size(max = 50, message = "화면 표시용 사이즈 값은 최대 50자입니다.")
        String displayLabel,

        @NotNull(message = "정렬 값(sortId)은 필수입니다.")
        Long sortId
) {}
