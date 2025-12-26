package com.bidket.product.application.resolver;

import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductDetailResolverRegistry {

    private final List<ProductDetailResolver> resolvers;

    public ProductDetailResolverRegistry(
            List<ProductDetailResolver> resolvers
    ) {
        this.resolvers = resolvers;
    }

    public ProductDetailResolver getResolver(ProductType productType) {
        return resolvers.stream()
                .filter(r -> r.supports(productType))
                .findFirst()
                .orElseThrow(() ->
                        new ProductException(
                                ProductErrorCode.PRODUCT_DETAIL_RESOLVER_NOT_FOUND
                        )
                );
    }
}
