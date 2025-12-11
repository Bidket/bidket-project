package com.bidket.product.application.service;

import com.bidket.common.presentation.response.PageResponse;
import com.bidket.product.application.mapper.CategoryMapper;
import com.bidket.product.application.mapper.SkuMapper;
import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.domain.model.SkuStatus;
import com.bidket.product.infrastructure.persistence.entity.Category;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductCategory;
import com.bidket.product.infrastructure.persistence.entity.ProductSku;
import com.bidket.product.infrastructure.persistence.repository.ProductCategoryRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductSkuRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductSpecification;
import com.bidket.product.presentation.dto.request.PageRequestDto;
import com.bidket.product.presentation.dto.request.product.SkuGetRequest;
import com.bidket.product.presentation.dto.response.category.CategoryGetResponse;
import com.bidket.product.presentation.dto.response.product.SkuGetResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductSkuRepository skuRepository;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;

    /** 상품 단건 조회 */
    public Product getActiveProduct(UUID productId) {

        return productRepository.findOne(
                Specification.where(ProductSpecification.hasId(productId))
                        .and(ProductSpecification.isActiveProduct())
                        .and(ProductSpecification.notDeleted())
        ).orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    /** 상품의 카테고리 조회 */
    public List<CategoryGetResponse> getCategoriesByProduct(Product product) {

        List<ProductCategory> productCategories =
                productCategoryRepository.findByProduct(product);

        // 상품은 하나 이상의 카테고리를 가져야 함
        if (productCategories.isEmpty()) {
            throw new ProductException(ProductErrorCode.PRODUCT_CATEGORY_NOT_SET);
        }

        List<Category> categories = productCategories.stream()
                .map(ProductCategory::getCategory)
                .toList();

        return categoryMapper.toGetResponseList(categories);
    }

    /** SKU 단건 조회 */
    public SkuGetResponse getSku(UUID skuId) {

        ProductSku sku = skuRepository.findByIdAndStatusAndDeletedAtIsNull(
                skuId,
                SkuStatus.ACTIVE
        ).orElseThrow(() -> new ProductException(ProductErrorCode.SKU_NOT_FOUND));

        return skuMapper.toGetResponse(sku);
    }

    /** SKU 목록 조회 */
    public PageResponse<SkuGetResponse> getSkuList(
            SkuGetRequest req,
            PageRequestDto pageRequest) {
        Pageable pageable = pageRequest.toPageable();

        // 상품 존재 여부 확인
        Product product = getActiveProduct(req.productId());

        Page<ProductSku> page =
                skuRepository.findByProductIdAndStatusAndDeletedAtIsNull(
                        req.productId(),
                        SkuStatus.ACTIVE,
                        pageable
                );

        List<SkuGetResponse> resList = page.getContent().stream()
                .map(skuMapper::toGetResponse)
                .toList();

        return PageResponse.of(
                resList,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    /** 상품의 SKU 조회 */
    public List<SkuGetResponse> getSkusByProductId(UUID productId) {

        // 상품 존재 여부 확인
        Product product = getActiveProduct(productId);

        return skuRepository.findByProductIdAndStatusAndDeletedAtIsNull(productId, SkuStatus.ACTIVE)
                .stream()
                .map(skuMapper::toGetResponse)
                .toList();
    }
}
