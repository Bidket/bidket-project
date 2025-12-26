package com.bidket.product.infrastructure.persistence.repository;

import com.bidket.product.domain.model.BrandStatus;
import com.bidket.product.infrastructure.persistence.entity.Brand;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    Optional<Brand> findByName(String name);

    Optional<Brand> findByNameKr(String nameKr);

    List<Brand> findAllByStatus(BrandStatus status);
}
