package com.bidket.product.application.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bidket.product.common.TestFixture;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.domain.model.Silhouette;
import com.bidket.product.infrastructure.persistence.entity.*;
import com.bidket.product.presentation.dto.request.shoesdetail.ProductShoesDetailCreateRequest;
import com.bidket.product.presentation.dto.response.shoesdetail.ProductShoesDetailCreateResponse;
import java.math.BigDecimal;
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
public class ShoesDetailServiceIntegrationTest {

    @Autowired
    ShoesDetailService shoesDetailService;
    @Autowired
    TestFixture fixture;

    ProductType pt;
    Brand brand;

    @BeforeEach
    void setup() {
        pt = fixture.createProductType();
        brand = fixture.createBrand();
    }

    @Test
    void createShoesDetail_success() {

        Product product = fixture.createProduct(pt, brand);

        ProductShoesDetailCreateRequest req =
                new ProductShoesDetailCreateRequest(
                        "Black",
                        "Leather",
                        Silhouette.HIGH,
                        "Classic",
                        "Vietnam",
                        new BigDecimal("300")
                );

        ProductShoesDetailCreateResponse res =
                shoesDetailService.createShoesDetail(product.getId(), req);

        assertNotNull(res);
    }

    @Test
    void createShoesDetail_fail_whenProductNotExists() {

        ProductShoesDetailCreateRequest req =
                new ProductShoesDetailCreateRequest(
                        "Black",
                        "Leather",
                        Silhouette.HIGH,
                        "Classic",
                        "Vietnam",
                        new BigDecimal("300")
                );

        assertThrows(ProductException.class,
                () -> shoesDetailService.createShoesDetail(UUID.randomUUID(), req));
    }
}
