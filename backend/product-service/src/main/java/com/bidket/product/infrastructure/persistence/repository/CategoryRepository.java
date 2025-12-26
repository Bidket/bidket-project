package com.bidket.product.infrastructure.persistence.repository;

import com.bidket.product.infrastructure.persistence.entity.Category;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByParent_Id(UUID parentId);

    List<Category> findAllByDepth(int depth);

    @EntityGraph(attributePaths = "parent")
    @Query("select c from Category c")
    List<Category> findAllWithParent();

    boolean existsByNameAndParentAndProductType(
            String name, Category parent, ProductType productType
    );

    List<Category> findAllByParent(Category parent);

    Long countByParent(Category parent);
}
