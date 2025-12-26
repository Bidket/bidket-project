package com.bidket.product.presentation.dto.request.brand;

import com.bidket.product.domain.model.BrandStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "브랜드 상태 변경 요청")
public record BrandStatusChangeRequest(
        @Schema(
                description = "브랜드 상태",
                example = "INACTIVE",
                required = true
        )
        BrandStatus status
) {}
