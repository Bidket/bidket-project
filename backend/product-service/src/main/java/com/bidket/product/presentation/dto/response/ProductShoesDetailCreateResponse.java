package com.bidket.product.presentation.dto.response;

import com.bidket.product.domain.model.Silhouette;
import com.bidket.product.infrastructure.persistence.entity.ProductShoesDetail;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductShoesDetailCreateResponse(
        UUID id,
        UUID productId,
        String colorway,
        String mainMaterial,
        Silhouette silhouette,
        String style,
        String originCountry,
        BigDecimal weight
) {

    public static ProductShoesDetailCreateResponse from(ProductShoesDetail detail) {
        return new ProductShoesDetailCreateResponse(
                detail.getId(),
                detail.getProductId(),
                detail.getColorway(),
                detail.getMainMaterial(),
                detail.getSilhouette(),
                detail.getStyle(),
                detail.getOriginCountry(),
                detail.getWeight()
        );
    }
}
