package com.bidket.product.presentation.dto.response.product;

import com.bidket.product.presentation.dto.response.shoesdetail.ProductShoesDetailCreateResponse;

public record ProductWithShoesCreateResponse(
        ProductCreateResponse product,
        ProductShoesDetailCreateResponse shoesDetail
) {
    public static ProductWithShoesCreateResponse of(
            ProductCreateResponse product,
            ProductShoesDetailCreateResponse detail
    ) {
        return new ProductWithShoesCreateResponse(product, detail);
    }
}
