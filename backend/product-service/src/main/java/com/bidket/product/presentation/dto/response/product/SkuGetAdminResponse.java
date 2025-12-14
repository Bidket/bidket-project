package com.bidket.product.presentation.dto.response.product;

import com.bidket.product.domain.model.SkuStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record SkuGetAdminResponse (
        UUID skuId,
        String skuCode,

        UUID sizeId,
        String sizeLabel,

        SkuStatus status,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
