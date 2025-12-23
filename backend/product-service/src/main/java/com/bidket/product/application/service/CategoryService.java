package com.bidket.product.application.service;

import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.Category;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.infrastructure.persistence.repository.CategoryRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductTypeRepository;
import com.bidket.product.presentation.dto.request.category.CategoryCreateRequest;
import com.bidket.product.presentation.dto.response.category.CategoryCreateResponse;
import com.bidket.product.presentation.dto.response.category.CategoryGetResponse;
import com.bidket.product.presentation.dto.response.category.CategoryTreeResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final ProductTypeRepository productTypeRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
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

    @Transactional(readOnly = true)
    public List<CategoryGetResponse> getCategories(
            UUID parentId,
            Integer depth
    ) {
        List<Category> categories;

        if (parentId != null) {
            categories = categoryRepository.findAllByParent_Id(parentId);
        } else if (depth != null) {
            categories = categoryRepository.findAllByDepth(depth);
        } else {
            // parentId, depth 없으면 최상위 카테고리 조회
            categories = categoryRepository.findAllByDepth(0);
        }

        return categories.stream()
                .map(category -> new CategoryGetResponse(
                        category.getId(),
                        category.getDepth(),
                        category.getName()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree() {

        List<Category> categories = categoryRepository.findAllWithParent();

        // id → DTO 맵 생성
        Map<UUID, CategoryTreeResponse> map = new HashMap<>();

        categories.forEach(category ->
                map.put(
                        category.getId(),
                        new CategoryTreeResponse(
                                category.getId(),
                                category.getName(),
                                category.getDepth(),
                                new ArrayList<>()
                        )
                )
        );

        // 트리 조립
        List<CategoryTreeResponse> roots = new ArrayList<>();

        categories.forEach(category -> {
            CategoryTreeResponse current = map.get(category.getId());

            if (category.getParent() == null) {
                roots.add(current);
            } else {
                CategoryTreeResponse parent =
                        map.get(category.getParent().getId());
                parent.children().add(current);
            }
        });

        return roots;
    }
}
