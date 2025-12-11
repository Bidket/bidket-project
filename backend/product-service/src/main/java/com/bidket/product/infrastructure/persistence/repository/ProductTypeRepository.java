package com.bidket.product.infrastructure.persistence.repository;

import com.bidket.product.infrastructure.persistence.entity.ProductType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTypeRepository extends JpaRepository<ProductType, UUID> {

}
