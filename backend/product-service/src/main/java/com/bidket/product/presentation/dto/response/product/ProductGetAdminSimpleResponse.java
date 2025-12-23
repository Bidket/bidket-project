package com.bidket.product.presentation.dto.response.product;

import com.bidket.product.domain.model.Gender;
import com.bidket.product.domain.model.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductGetAdminSimpleResponse(
        UUID productId,
        String name,
        String nameKr,
        String modelCode,
        Gender gender,
        String description,
        LocalDate releaseDate,
        BigDecimal releasePrice,
        ProductStatus status,

        UUID productTypeId,
        UUID brandId,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {}
