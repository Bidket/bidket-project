package com.bidket.product.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bidket.product.common.TestFixture;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.Brand;
import com.bidket.product.infrastructure.persistence.repository.BrandRepository;
import com.bidket.product.presentation.dto.request.brand.BrandCreateRequest;
import com.bidket.product.presentation.dto.response.brand.BrandCreateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("local")
public class BrandServiceIntegrationTest {

    @Autowired
    BrandService brandService;

    @Autowired
    BrandRepository brandRepository;

    @Autowired
    TestFixture fixture;

    @Test
    void createBrand_success() {

        BrandCreateRequest req = new BrandCreateRequest(
                "Nike",
                "나이키",
                "US",
                "https://nike.com"
                //BrandStatus.ACTIVE
        );

        BrandCreateResponse res = brandService.createBrand(req);

        assertNotNull(res);
        assertEquals("Nike", res.name());
    }

    @Test
    void createBrand_fail_whenDuplicateName() {

        // Given
        Brand existing = fixture.createBrand();

        BrandCreateRequest req = new BrandCreateRequest(
                existing.getName(),
                "중복",
                "USA",
                null
                //BrandStatus.ACTIVE
        );

        // When & Then
        assertThrows(ProductException.class,
                () -> brandService.createBrand(req)
        );
    }
}
