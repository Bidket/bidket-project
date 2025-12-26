package com.bidket.product.application.resolver;

import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.domain.model.ProductDetail;
import com.bidket.product.infrastructure.persistence.entity.ProductShoesDetail;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.infrastructure.persistence.repository.ProductShoesDetailRepository;
import com.bidket.product.presentation.dto.request.product.ProductDetailUpdateRequest;
import com.bidket.product.presentation.dto.request.shoesdetail.ProductShoesDetailUpdateRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductShoesDetailResolver implements ProductDetailResolver {

    private final ProductShoesDetailRepository shoesDetailRepository;

    @Override
    public Boolean supports(ProductType productType) {
        return "SHOES".equals(productType.getCode());
    }

    @Override
    public ProductDetail resolve(UUID productId) {
        return shoesDetailRepository.findByProduct_Id(productId)
                .orElse(null);
    }

    @Override
    @Transactional
    public void update(UUID productId, ProductDetailUpdateRequest req) {
        ProductShoesDetail detail =
                shoesDetailRepository.findByProduct_Id(productId)
                        .orElseThrow(() -> new ProductException(ProductErrorCode.SHOES_DETAIL_NOT_FOUND));

        // 공통 상품 상세 -> 신발 상품 상세 캐스팅
        ProductShoesDetailUpdateRequest r = (ProductShoesDetailUpdateRequest) req;

        detail.updateInfo(
                r.colorway(),
                r.mainMaterial(),
                r.silhouette(),
                r.style(),
                r.originCountry(),
                r.weight()
        );
    }
}
