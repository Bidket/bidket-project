package com.bidket.product.presentation.dto.response;

import com.bidket.product.domain.model.BrandStatus;
import com.bidket.product.infrastructure.persistence.entity.Brand;
import java.util.UUID;

public record BrandCreateResponse(
        UUID id,
        String name,
        String nameKr,
        String originCountry,
        String websiteUrl,
        BrandStatus status
) {
    public static BrandCreateResponse from(Brand brand) {
        return new BrandCreateResponse(
                brand.getId(),
                brand.getName(),
                brand.getNameKr(),
                brand.getOriginCountry(),
                brand.getWebsiteUrl(),
                brand.getStatus()
        );
    }
}
