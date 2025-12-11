package com.bidket.product.presentation.dto.response.product;

import java.util.UUID;

public record SkuGetResponse(
        UUID id,
        UUID productId,
        UUID sizeId,
        String skuCode,
        String status
) {}
