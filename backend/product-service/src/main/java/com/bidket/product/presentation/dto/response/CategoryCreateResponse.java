package com.bidket.product.presentation.dto.response;

import com.bidket.product.infrastructure.persistence.entity.Category;
import java.util.UUID;

public record CategoryCreateResponse(
        UUID id,
        UUID productTypeId,
        UUID parentId,
        Integer depth,
        String name,
        String code,
        Long sortId,
        Boolean isLeaf
) {
    public static CategoryCreateResponse from(Category category) {
        return new CategoryCreateResponse(
                category.getId(),
                category.getProductType().getId(),
                category.getParentId() != null ? category.getParentId().getId() : null,
                category.getDepth(),
                category.getName(),
                category.getCode(),
                category.getSortId(),
                category.getIsLeaf()
        );
    }
}
