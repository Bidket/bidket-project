package com.bidket.product.application.facade;

import static org.junit.jupiter.api.Assertions.*;

import com.bidket.product.application.service.ShoesDetailService;
import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.domain.model.Gender;
import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.domain.model.Silhouette;
import com.bidket.product.infrastructure.persistence.entity.Brand;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductShoesDetail;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.infrastructure.persistence.repository.BrandRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductShoesDetailRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductTypeRepository;
import com.bidket.product.presentation.dto.request.product.ProductCreateRequest;
import com.bidket.product.presentation.dto.request.shoesdetail.ProductShoesDetailCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductWithShoesCreateRequest;
import com.bidket.product.presentation.dto.response.product.ProductWithShoesCreateResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class ProductAdminFacadeIntegrationTest {

    @Autowired
    ProductAdminFacade facade;

    @Autowired
    ShoesDetailService shoesDetailService;

    @Autowired
    BrandRepository brandRepository;

    @Autowired
    ProductTypeRepository productTypeRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductShoesDetailRepository shoesRepository;

    // 테스트용 fixture 생성
    private UUID savedProductTypeId;
    private UUID savedBrandId;

    @BeforeEach
    void setup() {
        // 1. 상품타입 생성
        ProductType pt = ProductType.of(
                "SNEAKERS",
                "스니커즈",
                "신발 유형"
        );
        pt = productTypeRepository.save(pt);
        savedProductTypeId = pt.getId();

        // 2. 브랜드 생성
        Brand brand = Brand.of(
                "Nike",
                "나이키",
                "US",
                null
        );
        brand = brandRepository.save(brand);
        savedBrandId = brand.getId();
    }


    @Test
    void createProductWithShoes_success() {
        // given
        // 1. product req 생성
        ProductCreateRequest productReq = new ProductCreateRequest(
                savedProductTypeId,
                savedBrandId,
                "Nike Air Max 97",
                "에어맥스 97",
                "AM97",
                Gender.UNISEX,
                "설명",
                LocalDate.now(),
                new BigDecimal("199000"),
                ProductStatus.ACTIVE
        );

        // 2. shoesDetail req 생성
        ProductShoesDetailCreateRequest shoesReq =
                new ProductShoesDetailCreateRequest(
                        "Triple White",
                        "Leather",
                        Silhouette.LOW,
                        "Classic",
                        "Vietnam",
                        new BigDecimal("350")
                );

        ProductWithShoesCreateRequest bundleReq =
                new ProductWithShoesCreateRequest(productReq, shoesReq);

        // when
        ProductWithShoesCreateResponse res = facade.createProductWithShoes(bundleReq);

        // then
        // 응답 검증
        assertNotNull(res);
        assertNotNull(res.product());
        assertNotNull(res.shoesDetail());
        assertNotNull(res.product().id());

        UUID productId = res.product().id();

        // db 조회 검증
        Product product = productRepository.findById(productId).orElse(null);
        assertNotNull(product);

        ProductShoesDetail detail = shoesRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new IllegalStateException("shoesDetail not found"));
        assertNotNull(detail);

        // 세부 검증
        assertEquals("Nike Air Max 97", product.getName());
        assertEquals("Triple White", detail.getColorway());
    }

    @Test
    void createShoesDetail_fail_whenProductNotFound() {

        // given
        UUID invalidProductId = UUID.randomUUID();

        ProductShoesDetailCreateRequest req =
                new ProductShoesDetailCreateRequest(
                        "Triple White",
                        "Leather",
                        Silhouette.LOW,
                        "Classic",
                        "Vietnam",
                        new BigDecimal("350")
                );

        // when & then
        ProductException ex = assertThrows(
                ProductException.class,
                () -> shoesDetailService.createShoesDetail(invalidProductId, req)
        );

        assertEquals(ProductErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
    }
}
