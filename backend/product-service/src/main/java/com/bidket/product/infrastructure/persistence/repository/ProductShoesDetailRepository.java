package com.bidket.product.infrastructure.persistence.repository;

import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductShoesDetail;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductShoesDetailRepository extends JpaRepository<ProductShoesDetail, UUID> {

    Optional<ProductShoesDetail> findByProduct_Id(UUID productId);

    Optional<ProductShoesDetail> findByProduct(Product product);
}
