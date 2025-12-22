package com.bidket.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bidket.product.common.TestFixture;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.Category;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.infrastructure.persistence.repository.CategoryRepository;
import com.bidket.product.presentation.dto.request.category.CategoryCreateRequest;
import com.bidket.product.presentation.dto.request.category.CategoryUpdateRequest;
import com.bidket.product.presentation.dto.response.category.CategoryCreateResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class CategoryIntegrationTest {

    @Autowired
    CategoryService categoryService;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    TestFixture fixture;

    ProductType pt;

    @BeforeEach
    void setup() {
        pt = fixture.createProductType();
    }

    @Test
    void createCategory_success() {

        CategoryCreateRequest req = new CategoryCreateRequest(
                pt.getId(),
                null,
                "조던",
                "JORDAN",
                100L
        );

        CategoryCreateResponse res = categoryService.createCategory(req);

        assertNotNull(res);
    }

    @Test
    void createCategory_fail_whenParentNotExists() {

        ProductType pt = fixture.createProductType();

        UUID invalidParentId = UUID.randomUUID();

        CategoryCreateRequest req = new CategoryCreateRequest(
                pt.getId(),
                invalidParentId,
                "테스트",
                "TEST",
                100L
        );

        assertThrows(ProductException.class,
                () -> categoryService.createCategory(req));
    }

    @Test
    void updateCategory_name_success() {
        // given
        Category category = fixture.createCategory(pt);

        CategoryUpdateRequest req =
                new CategoryUpdateRequest("에어조던", 200L);

        // when
        categoryService.updateCategory(category.getId(), req);

        // then
        Category updated = categoryRepository.findById(category.getId()).get();

        assertThat(updated.getName()).isEqualTo("에어조던");
        assertThat(updated.getSortId()).isEqualTo(200L);
    }

    @Test
    void updateCategory_parent_move_success() {
        // given
        Category root = fixture.createCategory(pt);

        Category parentA = Category.create(pt, root, "A", "A", 1L);
        categoryRepository.save(parentA);

        Category parentB = Category.create(pt, root, "B", "B", 2L);
        categoryRepository.save(parentB);

        Category child = Category.create(pt, parentA, "A1", "A1", 1L);
        categoryRepository.save(child);

        // when
        categoryService.moveCategory(child.getId(), parentB.getId());

        // then
        Category moved = categoryRepository.findById(child.getId()).get();

        assertThat(moved.getParent().getId()).isEqualTo(parentB.getId());
        assertThat(moved.getDepth()).isEqualTo(parentB.getDepth() + 1);
    }

    @Test
    void updateCategory_move_children_fail() {
        // given
        Category root = fixture.createCategory(pt);

        Category child = Category.create(
                pt,
                root,
                "미드탑",
                "MID",
                100L
        );
        categoryRepository.save(child);

        // when & then
        assertThatThrownBy(() ->
                categoryService.moveCategory(root.getId(), child.getId())
        ).isInstanceOf(ProductException.class)
                .hasMessageContaining("카테고리는 자기 자신이나 하위");
    }

    @Test
    void updateCategory_move_self_fail() {
        // given
        Category category = fixture.createCategory(pt);

        // when & then
        assertThatThrownBy(() ->
                categoryService.moveCategory(category.getId(), category.getId())
        ).isInstanceOf(ProductException.class)
                .hasMessageContaining("카테고리는 자기 자신이나 하위");
    }
}
