package com.bidket.product.infrastructure.persistence.repository;

import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {

    @Modifying
    @Query("UPDATE ProductCategory pc SET pc.isPrimary = false WHERE pc.product.id = :productId")
    void resetPrimary(UUID productId);

    List<ProductCategory> findByProduct(Product product);

    List<ProductCategory> findAllByProduct_Id(UUID product_id);

    List<ProductCategory> findAllByProduct_IdAndIsPrimaryTrue(UUID productId);

    void deleteAllByProduct(Product product);

    List<ProductCategory> findAllByProduct(Product product);
}
