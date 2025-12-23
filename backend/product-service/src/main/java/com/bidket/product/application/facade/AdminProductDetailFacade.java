package com.bidket.product.application.facade;

import com.bidket.product.application.mapper.AdminProductDetailMapper;
import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductCategory;
import com.bidket.product.infrastructure.persistence.entity.ProductShoesDetail;
import com.bidket.product.infrastructure.persistence.entity.ProductSku;
import com.bidket.product.infrastructure.persistence.repository.ProductCategoryRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductShoesDetailRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductSkuRepository;
import com.bidket.product.presentation.dto.response.product.ProductGetAdminDetailResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductDetailFacade {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductShoesDetailRepository shoesDetailRepository;

    private final AdminProductDetailMapper mapper;

    public ProductGetAdminDetailResponse getProductDetail(UUID productId) {

        // 상품 단건 조회(모든 상태)
        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 상품에 속한 카테고리 조회
        List<ProductCategory> productCategories =
                productCategoryRepository.findAllByProduct_Id(productId);

        // 상품에 속한 sku(모든 상태)
        List<ProductSku> skus =
                productSkuRepository.findAllByProduct_Id(productId);

        // 신발 상세 (없을 수도 있음)
        ProductShoesDetail shoesDetail =
                shoesDetailRepository.findByProduct_Id(productId)
                        .orElse(null);

        return mapper.toDetailResponse(
                product,
                productCategories,
                skus,
                shoesDetail
        );
    }
}
