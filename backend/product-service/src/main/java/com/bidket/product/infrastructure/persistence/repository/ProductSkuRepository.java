package com.bidket.product.infrastructure.persistence.repository;

import com.bidket.product.domain.model.SkuStatus;
import com.bidket.product.infrastructure.persistence.entity.ProductSku;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSkuRepository extends JpaRepository<ProductSku, UUID> {

    boolean existsBySkuCode(String skuCode);

    Optional<ProductSku> findByIdAndStatusAndDeletedAtIsNull(UUID skuId, SkuStatus status);

    List<ProductSku> findByProductIdAndStatusAndDeletedAtIsNull(UUID productId,SkuStatus status);

    Page<ProductSku> findByProductIdAndStatusAndDeletedAtIsNull(
            UUID productId, SkuStatus status, Pageable pageable);

    Page<ProductSku> findAllByProduct_Id(UUID productId, Pageable pageable);

    List<ProductSku> findAllByProduct_Id(UUID productId);

    List<ProductSku> findAllByProduct_IdAndStatus(UUID productId, SkuStatus status);
}
