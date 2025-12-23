package com.bidket.product.application.resolver;

import com.bidket.product.domain.model.ProductDetail;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.infrastructure.persistence.repository.ProductShoesDetailRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
