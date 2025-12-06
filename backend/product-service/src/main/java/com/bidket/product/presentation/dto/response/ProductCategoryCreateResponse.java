package com.bidket.product.presentation.dto.response;

import com.bidket.product.infrastructure.persistence.entity.ProductCategory;
import java.util.UUID;

public record ProductCategoryCreateResponse(
        UUID id,
        UUID productId,
        UUID categoryId,
        Boolean isPrimary
) {
    public static ProductCategoryCreateResponse from(ProductCategory productCategory)
    {
        return new ProductCategoryCreateResponse(
                productCategory.getId(),
                productCategory.getProduct().getId(),
                productCategory.getCategory().getId(),
                productCategory.getIsPrimary()
        );
    }
}
