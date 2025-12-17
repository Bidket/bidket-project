package com.bidket.product.infrastructure.persistence.repository;

import com.bidket.product.domain.model.BrandStatus;
import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.infrastructure.persistence.entity.Brand;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductCategory;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    public static Specification<Product> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }

            // 조인
            Join<Product, Brand> brand = root.join("brand", JoinType.LEFT);

            String like = "%" + keyword.trim().toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("nameKr")), like),
                    cb.like(cb.lower(root.get("modelCode")), like),
                    cb.like(cb.lower(brand.get("name")), like),
                    cb.like(cb.lower(brand.get("nameKr")), like)
            );
        };
    }

    public static Specification<Product> distinct() {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.conjunction();
        };
    }

    public static Specification<Product> notDeleted() {
        return (root, query, cb) ->
                cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Product> isActiveProduct() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), ProductStatus.ACTIVE);
    }

    public static Specification<Product> hasActiveBrand() {
        return (root, query, cb) -> {
            Join<Product, Brand> brand = root.join("brand", JoinType.INNER);
            return cb.equal(brand.get("status"), BrandStatus.ACTIVE);
        };
    }

    public static Specification<Product> hasId(UUID id) {
        return (root, query, cb) ->
                cb.equal(root.get("id"), id);
    }

    /** 현재 단일 품목 "SHOES"만 사용 */
    public static Specification<Product> hasProductType(UUID productTypeId) {
        return (root, query, cb) -> {
            if (productTypeId == null) return null;
            return cb.equal(root.get("productType").get("id"), productTypeId);
        };
    }

    public static Specification<Product> hasBrand(UUID brandId) {
        return (root, query, cb) -> {
          if (brandId == null) return null;
          return cb.equal(root.get("brand").get("id"), brandId);
        };
    }

    public static Specification<Product> hasGender(String gender) {
        return (root, query, cb) -> {
            if (gender == null || gender.isBlank()) return null;
            return cb.equal(root.get("gender"), gender);
        };
    }

    public static Specification<Product> betweenPrice(
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) return null;

            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("releasePrice"), minPrice, maxPrice);
            } else if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("releasePrice"), minPrice);
            } else {
                return cb.lessThanOrEqualTo(root.get("releasePrice"), maxPrice);
            }
        };
    }

    /** 대표 카테고리 기반 필터링(ProductCategory.isPrimary = true 매핑만 사용) */
    public static Specification<Product> hasPrimaryCategory(UUID categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return null;

            // ProductCategory(상품과 카테고리 매핑) 조인
            Join<Product, ProductCategory> join =
                    root.join("productCategories", JoinType.INNER);

            return cb.and(
                    cb.equal(join.get("category").get("id"), categoryId),
                    cb.isTrue(join.get("isPrimary"))
            );
        };
    }
}
