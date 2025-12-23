package com.bidket.product.application.facade;

import com.bidket.product.application.mapper.ProductPageMapper;
import com.bidket.product.application.mapper.SkuMapper;
import com.bidket.product.application.resolver.ProductDetailResolver;
import com.bidket.product.application.service.ProductQueryService;
import com.bidket.product.application.service.ShoesDetailService;
import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.domain.model.ProductDetail;
import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.domain.model.SkuStatus;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductCategory;
import com.bidket.product.infrastructure.persistence.entity.ProductShoesDetail;
import com.bidket.product.infrastructure.persistence.entity.ProductSku;
import com.bidket.product.infrastructure.persistence.repository.ProductCategoryRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductSkuRepository;
import com.bidket.product.presentation.dto.response.category.CategoryGetResponse;
import com.bidket.product.presentation.dto.response.product.ProductPageGetResponse;
import com.bidket.product.presentation.dto.response.product.ProductPageGetResponse.ShoesDetailInfo;
import com.bidket.product.presentation.dto.response.product.SkuGetResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPageQueryFacade {

    private final ProductQueryService productQueryService;
    private final ShoesDetailService shoesDetailService;
    private final ProductPageMapper productPageMapper;

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductSkuRepository productSkuRepository;

    private final List<ProductDetailResolver> detailResolvers;
    private final SkuMapper skuMapper;

    public ProductPageGetResponse getPage(UUID productId) {

        // 1. Product 엔티티 조회
        Product product = productQueryService.getActiveProduct(productId);

        // 2. ShoesDetail 조회 -> DTO 변환 -> ProductPage DTO 변환
        ProductShoesDetail shoesDetailDto = shoesDetailService.getShoesDetail(product);

        ShoesDetailInfo shoesDetailInfo = productPageMapper.toShoesDetailInfo(shoesDetailDto);

        // 3. Category 조회 -> DTO 변환 -> CategoryInfo 변환
        List<CategoryGetResponse> categoryDtos =
                productQueryService.getCategoriesByProduct(product);

        List<ProductPageGetResponse.CategoryInfo> categoryInfos =
                categoryDtos.stream()
                        .map(category -> new ProductPageGetResponse.CategoryInfo(
                                category.id(),
                                category.name(),
                                category.depth()
                        ))
                        .toList();

        // 4. SKU 조회 -> SkuGetResponse 반환
        List<SkuGetResponse> skuDtos =
                productQueryService.getSkusByProductId(productId);

        // 5. 조합해서 ProductPage DTO 생성
        return productPageMapper.toPageResponse(
                product,
                shoesDetailInfo,
                categoryInfos,
                skuDtos
        );
    }

    public ProductPageGetResponse getProductDetail(UUID productId) {

        // 1. ACTIVE 상품 조회
        Product product = productRepository.findById(productId)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() ->
                        new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND)
                );

        // 2. 대표 카테고리 조회
        List<ProductCategory> categories =
                productCategoryRepository
                        .findAllByProduct_IdAndIsPrimaryTrue(productId);

        // 3. ACTIVE SKU 조회
        List<ProductSku> skus =
                productSkuRepository
                        .findAllByProduct_IdAndStatus(productId, SkuStatus.ACTIVE);

        // sku -> dto 변환
        List<SkuGetResponse> skuDtos = skuMapper.toGetResponseList(skus);

        // 4. 상품 타입 기반 상세 Resolver 선택
        ProductDetail productDetail = detailResolvers.stream()
                .filter(r -> r.supports(product.getProductType()))
                .findFirst()
                .map(r -> r.resolve(productId))
                .orElse(null);

        return productPageMapper.toPageResponse(
                product,
                productPageMapper.mapShoesDetail(productDetail),
                productPageMapper.mapCategories(categories),
                skuDtos
        );
    }
}
