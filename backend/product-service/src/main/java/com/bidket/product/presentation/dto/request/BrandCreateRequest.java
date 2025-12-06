package com.bidket.product.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BrandCreateRequest(

        @NotBlank(message = "브랜드 이름은 필수입니다.")
        @Pattern(regexp = "^[a-zA-Z]+$", message = "영어 대소문자만 입력가능합니다.")
        @Size(max = 100, message = "브랜드 이름은 최대 100자입니다.")
        String name,

        @Pattern(regexp = "^[가-힣\\s]+$", message = "공백과 한글만 입력가능합니다.")
        @Size(max = 100, message = "브랜드 한글명은 최대 100자입니다.")
        String nameKr,

        @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "공백과 영어 대소문자만 입력가능합니다.")
        @Size(max = 50, message = "브랜드 설립 국가명은 최대 50자입니다.")
        String originCountry,

        @Pattern(
                regexp = "^(http|https)://[a-zA-Z0-9.-]+\\.[a-zA-Z0-9]{2,}(/[a-zA-Z0-9_.-]*)*$",
                message = "유효한 웹사이트 URL 형식이 아닙니다.")
        @Size(max = 200, message = "URL은 최대 200자입니다.")
        String websiteUrl
) {}
