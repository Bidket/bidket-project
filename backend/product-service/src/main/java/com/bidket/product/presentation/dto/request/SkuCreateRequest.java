package com.bidket.product.presentation.dto.request;

import com.bidket.product.domain.model.SkuStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SkuCreateRequest(

        @NotNull(message = "사이즈 ID는 필수입니다.")
        UUID sizeId,

        @NotBlank(message = "SKU 코드는 필수입니다.")
        @Pattern(
                regexp = "[A-Z0-9_-]+$",
                message = "영어 대문자와 숫자 특수문자 '-','_' 만 입력가능합니다.")
        @Size(max = 50, message = "SKU 코드는 최대 50자입니다.")
        String skuCode,

        @NotNull(message = "SKU 상태는 필수입니다.")
        SkuStatus status
) {}
