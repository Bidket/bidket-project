package com.bidket.product.presentation.dto.response.product;

import com.bidket.product.domain.model.ProductStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductGetAdminResponse(
        UUID productId,
        String name,
        String modelCode,

        ProductStatus status,

        UUID productTypeId,
        UUID brandId,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
