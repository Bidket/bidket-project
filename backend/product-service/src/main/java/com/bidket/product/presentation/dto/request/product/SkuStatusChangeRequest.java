package com.bidket.product.presentation.dto.request.product;

import com.bidket.product.domain.model.SkuStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sku 상태 수정 요청")
public record SkuStatusChangeRequest(
        @Schema(
                description = "sku 상태",
                example = "INACTIVE",
                required = true
        )
        SkuStatus status
) {}
