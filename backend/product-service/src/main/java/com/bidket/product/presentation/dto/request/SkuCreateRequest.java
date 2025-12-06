package com.bidket.product.presentation.dto.request;

import com.bidket.product.domain.model.SkuStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SkuCreateRequest(

        @NotNull(message = "상품 ID는 필수입니다.")
        UUID productId,

        @NotNull(message = "사이즈 ID는 필수입니다.")
        UUID sizeId,

        @NotBlank(message = "SKU 코드는 필수입니다.")
        @Size(max = 50, message = "SKU 코드는 최대 50자입니다.")
        String skuCode,

        @NotNull(message = "SKU 상태는 필수입니다.")
        SkuStatus status
) {}
