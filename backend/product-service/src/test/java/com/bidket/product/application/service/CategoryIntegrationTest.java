package com.bidket.product.application.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bidket.product.common.TestFixture;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.presentation.dto.request.CategoryCreateRequest;
import com.bidket.product.presentation.dto.response.CategoryCreateResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("local")
public class CategoryIntegrationTest {

    @Autowired
    CategoryService categoryService;
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
}
