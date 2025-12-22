package com.bidket.product.infrastructure.persistence.repository;

import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.infrastructure.persistence.entity.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends
        JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findById(UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Product p 
        set p.status = :status 
        where p.brand.id = :brandId
    """)
    int updateStatusByBrandId(
            @Param("brandId") UUID brandId,
            @Param("status") ProductStatus status
    );
}
