package com.bidket.product.application.service;

import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.Category;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.infrastructure.persistence.repository.CategoryRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductTypeRepository;
import com.bidket.product.presentation.dto.request.category.CategoryCreateRequest;
import com.bidket.product.presentation.dto.response.category.CategoryCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final ProductTypeRepository productTypeRepository;
    private final CategoryRepository categoryRepository;

    public CategoryCreateResponse createCategory(CategoryCreateRequest req) {

        ProductType productType = productTypeRepository.findById(req.productTypeId())
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_TYPE_NOT_FOUND));

        Category parent = null;
        if (req.parentId() != null) {
            parent = categoryRepository.findById(req.parentId())
                    .orElseThrow(() -> new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND));


            parent.markAsNotLeaf();
        }

        Category category = Category.create(
                productType,
                parent,
                req.name(),
                req.code(),
                req.sortId()
        );

        Category saved = categoryRepository.save(category);
        return CategoryCreateResponse.from(saved);
    }

}
