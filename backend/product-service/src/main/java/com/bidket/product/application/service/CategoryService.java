package com.bidket.product.application.service;

import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.Category;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.infrastructure.persistence.repository.CategoryRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductTypeRepository;
import com.bidket.product.presentation.dto.request.category.CategoryCreateRequest;
import com.bidket.product.presentation.dto.request.category.CategoryUpdateRequest;
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

    @Transactional
    public void updateCategory(UUID categoryId, CategoryUpdateRequest req) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND));

        // 같은 parent + 같은 productType 내 name 중복 확인
        if (req.name() != null && !req.name().equals(category.getName())) {
            boolean exists = categoryRepository.existsByNameAndParentAndProductType(
                    req.name(),
                    category.getParent(),
                    category.getProductType()
            );
            if (exists) {
                throw new ProductException(ProductErrorCode.CATEGORY_ALREADY_EXISTS);
            }
        }

        category.updateInfo(req.name(), req.sortId());
    }

    @Transactional
    public void moveCategory(UUID categoryId, UUID newParentId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND));

        Category newParent = null;
        if (newParentId != null) {
            newParent = categoryRepository.findById(newParentId)
                    .orElseThrow(() -> new ProductException(ProductErrorCode.PARENT_CATEGORY_NOT_FOUND));
        }

        // 자기 자신을 부모 설정 금지
        if (newParent != null && category.getId().equals(newParent.getId())) {
            throw new ProductException(ProductErrorCode.INVALID_CATEGORY_MOVE);
        }

        // 자식 밑으로 이동 금지 (트리 순환 방지)
        if (isDescendant(category, newParent)) {
            throw new ProductException(ProductErrorCode.INVALID_CATEGORY_MOVE);
        }

        Category oldParent = category.getParent();

        category.moveTo(newParent);

        // 기존 부모 leaf 재계산
        if (oldParent != null && categoryRepository.countByParent(oldParent) == 0) {
            oldParent.markAsLeaf();
        }

        // 새 부모 leaf 해제
        if (newParent != null) {
            newParent.markAsNotLeaf();
        }

        // 자식들 depth 재계산
        updateChildrenDepth(category);
    }

    // depth 재계산
    private void updateChildrenDepth(Category parent) {
        List<Category> children = categoryRepository.findAllByParent(parent);
        for (Category child : children) {
            child.updateDepth();
            updateChildrenDepth(child);
        }
    }

    // descendant 확인
    private boolean isDescendant(Category source, Category target) {
        Category current = target;
        while (current != null) {
            if (current.getId().equals(source.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
