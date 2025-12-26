package com.bidket.product.presentation.dto.request.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "브랜드 수정 요청")
public record BrandUpdateRequest(

        @Schema(description = "브랜드명", example = "Nike")
        @Pattern(regexp = "^[a-zA-Z]+$", message = "영어 대소문자만 입력가능합니다.")
        @Size(max = 100, message = "브랜드 이름은 최대 100자입니다.")
        String name,

        @Schema(description = "브랜드 한글명", example = "나이키")
        @Pattern(regexp = "^[가-힣\\s]+$", message = "공백과 한글만 입력가능합니다.")
        @Size(max = 100, message = "브랜드 한글명은 최대 100자입니다.")
        String nameKr,

        @Schema(description = "브랜드 설립 국가명", example = "US")
        @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "공백과 영어 대소문자만 입력가능합니다.")
        @Size(max = 50, message = "브랜드 설립 국가명은 최대 50자입니다.")
        String originCountry,

        @Schema(description = "공식 홈페이지 URL", example = "https://www.nike.com")
        @Pattern(
                regexp = "^(http|https)://[a-zA-Z0-9.-]+\\.[a-zA-Z0-9]{2,}(/[a-zA-Z0-9_.-]*)*$",
                message = "유효한 웹사이트 URL 형식이 아닙니다.")
        @Size(max = 200, message = "URL은 최대 200자입니다.")
        String websiteUrl
) {}
