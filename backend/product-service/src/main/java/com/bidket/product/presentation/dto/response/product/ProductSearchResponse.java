package com.bidket.product.presentation.dto.response.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSearchResponse(
        UUID id,
        String brandName,
        String name,
        String nameKr,
        String modelCode,
        BigDecimal releasePrice
) {}
