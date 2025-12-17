package com.bidket.product.presentation.dto.request.product;

import com.bidket.product.presentation.dto.request.shoesdetail.ProductShoesDetailCreateRequest;
import jakarta.validation.constraints.NotNull;

public record ProductWithShoesCreateRequest(

        @NotNull(message = "상품 정보는 필수입니다.")
        ProductCreateRequest product,

        @NotNull(message = "신발 상품 상세 정보는 필수입니다.")
        ProductShoesDetailCreateRequest shoesDetail
) {}
