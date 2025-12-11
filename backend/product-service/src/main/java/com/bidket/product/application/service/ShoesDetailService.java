package com.bidket.product.application.service;

import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductShoesDetail;
import com.bidket.product.infrastructure.persistence.repository.ProductRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductShoesDetailRepository;
import com.bidket.product.presentation.dto.request.shoesdetail.ProductShoesDetailCreateRequest;
import com.bidket.product.presentation.dto.response.shoesdetail.ProductShoesDetailCreateResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ShoesDetailService {

    private final ProductRepository productRepository;
    private final ProductShoesDetailRepository productShoesDetailRepository;

    public ProductShoesDetailCreateResponse createShoesDetail(
            UUID productId,
            ProductShoesDetailCreateRequest req
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

        ProductShoesDetail productShoesDetail = ProductShoesDetail.create(
                product,
                req.colorway(),
                req.mainMaterial(),
                req.silhouette(),
                req.style(),
                req.originCountry(),
                req.weight()
        );

        ProductShoesDetail saved = productShoesDetailRepository.save(productShoesDetail);
        return ProductShoesDetailCreateResponse.from(saved);
    }

    public ProductShoesDetail getShoesDetail(Product product) {
        return productShoesDetailRepository.findByProduct(product)
                .orElseThrow(() -> new ProductException(ProductErrorCode.SHOES_DETAIL_NOT_FOUND));
    }
}
