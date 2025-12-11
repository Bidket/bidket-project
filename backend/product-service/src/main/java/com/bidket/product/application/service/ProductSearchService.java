package com.bidket.product.application.service;

import com.bidket.common.presentation.response.PageResponse;
import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.domain.model.BrandStatus;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.repository.BrandRepository;
import com.bidket.product.infrastructure.persistence.repository.CategoryRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductSpecification;
import com.bidket.product.infrastructure.persistence.repository.ProductTypeRepository;
import com.bidket.product.presentation.dto.request.PageRequestDto;
import com.bidket.product.presentation.dto.request.product.ProductSearchRequest;
import com.bidket.product.presentation.dto.response.product.ProductSearchResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTypeRepository productTypeRepository;

    public PageResponse<ProductSearchResponse> search(
            ProductSearchRequest req,
            PageRequestDto pageReq
    ) {
        Pageable pageable = pageReq.toPageable();

        // 검색 조건 유효성 검사
        if (req.brandId() != null) {
            brandRepository.findById(req.brandId())
                    .filter(brand -> brand.getStatus() == BrandStatus.ACTIVE)
                    .orElseThrow(() -> new ProductException(ProductErrorCode.BRAND_NOT_FOUND));
        }

        if (req.categoryId() != null) {
            if (!categoryRepository.existsById(req.categoryId())) {
                throw new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND);
            }
        }

        if (req.productTypeId() != null) {
            if (!productTypeRepository.existsById(req.productTypeId())) {
                throw new ProductException(ProductErrorCode.PRODUCT_TYPE_NOT_FOUND);
            }
        }

        // 동적 where 조합
        Specification<Product> spec = Specification
                .where(ProductSpecification.distinct())
                .and(ProductSpecification.isActiveProduct())
                .and(ProductSpecification.notDeleted())
                .and(ProductSpecification.hasActiveBrand())
                .and(ProductSpecification.hasKeyword(req.keyword()))
                .and(ProductSpecification.hasBrand(req.brandId()))
                .and(ProductSpecification.hasProductType(req.productTypeId()))
                .and(ProductSpecification.hasGender(req.normalizedGender()))
                .and(ProductSpecification.betweenPrice(req.minPrice(), req.maxPrice()))
                .and(ProductSpecification.hasPrimaryCategory(req.categoryId()));

        // repository 실행
        Page<Product> page = productRepository.findAll(spec, pageable);

        // dto 변환
        List<ProductSearchResponse> content = page.getContent()
                .stream()
                .map(product -> new ProductSearchResponse(
                        product.getId(),
                        product.getBrand().getName(),
                        product.getName(),
                        product.getNameKr(),
                        product.getModelCode(),
                        product.getReleasePrice()
                ))
                .toList();

        return PageResponse.of(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
