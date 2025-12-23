package com.bidket.product.infrastructure.persistence.repository;

import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductCategory;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public class ProductPublicSpecification {

    /** ACTIVE 상품만 */
    public static Specification<Product> isActive() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), ProductStatus.ACTIVE);
    }

    /** 브랜드 필터 */
    public static Specification<Product> hasBrand(UUID brandId) {
        return (root, query, cb) -> {
            if (brandId == null) return cb.conjunction();
            return cb.equal(root.get("brand").get("id"), brandId);
        };
    }

    /** 대표 카테고리 기반 필터링(ProductCategory.isPrimary = true 매핑만 사용) */
    public static Specification<Product> hasPrimaryCategory(UUID categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return cb.conjunction();

            Join<Product, ProductCategory> join =
                    root.join("productCategories", JoinType.INNER);

            return cb.and(
                    cb.equal(join.get("category").get("id"), categoryId),
                    cb.isTrue(join.get("isPrimary"))
            );
        };
    }
}
