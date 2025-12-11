package com.bidket.product.presentation.dto.response.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductPageGetResponse(
        UUID id,
        String name,
        String nameKr,
        String modelCode,
        String gender,
        BigDecimal releasePrice,
        String brandName,
        String productTypeName,

        List<CategoryInfo> categories,
        List<SkuGetResponse> skus,
        ShoesDetailInfo shoesDetail
) {
    public record CategoryInfo(
            UUID id,
            String name,
            int depth
    ) {}

    public record ShoesDetailInfo(
            String colorway,
            String mainMaterial,
            String silhouette,
            String style,
            String originCountry,
            BigDecimal weight
    ) {}
}
