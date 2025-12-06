package com.bidket.product.presentation.dto.response;

import com.bidket.product.infrastructure.persistence.entity.ProductSku;
import java.util.UUID;

public record SkuCreateResponse(
        UUID id,
        UUID productId,
        UUID sizeId,
        String skuCode
) {
    public static SkuCreateResponse from(ProductSku sku) {
        return new SkuCreateResponse(
                sku.getId(),
                sku.getProduct().getId(),
                sku.getSize().getId(),
                sku.getSkuCode()
        );
    }
}
