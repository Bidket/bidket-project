package com.bidket.product.presentation.dto.response;

import com.bidket.product.infrastructure.persistence.entity.ProductType;
import java.util.UUID;

public record ProductTypeCreateResponse (
        UUID id,
        String code,
        String name,
        String description
) {
    public static ProductTypeCreateResponse from(ProductType type) {
        return new ProductTypeCreateResponse(
                type.getId(),
                type.getCode(),
                type.getName(),
                type.getDescription()
        );
    }
}
