package com.bidket.product.domain.repository;

import com.bidket.product.domain.model.ProductSku;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSkuRepository extends JpaRepository<ProductSku, UUID> {

}
