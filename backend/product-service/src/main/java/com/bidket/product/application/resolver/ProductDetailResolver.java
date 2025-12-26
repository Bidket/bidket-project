package com.bidket.product.application.resolver;

import com.bidket.product.domain.model.ProductDetail;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.presentation.dto.request.product.ProductDetailUpdateRequest;
import java.util.UUID;

public interface ProductDetailResolver {

    Boolean supports(ProductType productType);

    ProductDetail resolve(UUID productId);

    void update(UUID productId, ProductDetailUpdateRequest req);
}
