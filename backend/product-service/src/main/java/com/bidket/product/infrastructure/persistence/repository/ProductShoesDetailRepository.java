package com.bidket.product.infrastructure.persistence.repository;

import com.bidket.product.infrastructure.persistence.entity.ProductShoesDetail;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductShoesDetailRepository extends JpaRepository<ProductShoesDetail, UUID> {

}
