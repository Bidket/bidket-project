package com.bidket.product.presentation.dto.response;

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
