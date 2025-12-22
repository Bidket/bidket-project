package com.bidket.product.presentation.dto.request.product;

import com.bidket.product.domain.model.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 상태 변경 요청")
public record ProductStatusChangeRequest(
        @Schema(
                description = "상품 상태",
                example = "INACTIVE",
                required = true
        )
        ProductStatus status
) {}
