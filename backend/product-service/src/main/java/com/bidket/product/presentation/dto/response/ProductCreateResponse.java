package com.bidket.product.presentation.dto.response;

import com.bidket.product.domain.model.Gender;
import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.infrastructure.persistence.entity.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductCreateResponse(
        UUID id,
        String name,
        String modelCode,
        Gender gender,
        LocalDate releaseDate,
        BigDecimal releasePrice,
        String description,
        ProductStatus status
) {

    public static ProductCreateResponse from(Product product) {
        return new ProductCreateResponse(
                product.getId(),
                product.getName(),
                product.getModelCode(),
                product.getGender(),
                product.getReleaseDate(),
                product.getReleasePrice(),
                product.getDescription(),
                product.getStatus()
        );
    }
}
