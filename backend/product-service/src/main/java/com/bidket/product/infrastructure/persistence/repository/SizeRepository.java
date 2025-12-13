package com.bidket.product.infrastructure.persistence.repository;

import com.bidket.product.infrastructure.persistence.entity.Size;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeRepository extends JpaRepository<Size, UUID> {

    Page<Size> findAll(Pageable pageable);
    Page<Size> findAllBySizeType_Id(UUID sizeTypeId, Pageable pageable);
}
