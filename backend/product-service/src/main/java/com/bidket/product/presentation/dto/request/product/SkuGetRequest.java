package com.bidket.product.presentation.dto.request.product;

import java.util.UUID;

public record SkuGetRequest(
        UUID productId,
        UUID sizeId,
        String status
) {}
