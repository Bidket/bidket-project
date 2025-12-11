package com.bidket.product.infrastructure.persistence.repository;

import com.bidket.product.infrastructure.persistence.entity.SizeType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SizeTypeRepository extends JpaRepository<SizeType, UUID> {

    @Modifying
    @Query("UPDATE SizeType st SET st.isDefault = false WHERE st.productType.id = :productTypeId")
    void resetDefault(UUID productTypeId);

}
