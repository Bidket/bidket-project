package com.bidket.product.infrastructure.persistence.repository;

import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.infrastructure.persistence.entity.Product;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public class ProductAdminSpecification {

    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, query, cb) -> {
            if (status == null || status == ProductStatus.ALL) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Product> hasBrand(UUID brandId) {
        return (root, query, cb) -> {
            if (brandId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("brand").get("id"), brandId);
        };
    }

    public static Specification<Product> hasProductType(UUID productTypeId) {
        return (root, query, cb) -> {
            if (productTypeId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("productType").get("id"), productTypeId);
        };
    }
}
