package com.bidket.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.bidket.product.common.TestFixture;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.domain.model.*;
import com.bidket.product.infrastructure.persistence.entity.*;
import com.bidket.product.infrastructure.persistence.repository.ProductCategoryRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductShoesDetailRepository;
import com.bidket.product.presentation.dto.request.product.ProductCategoryCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductTypeCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductUpdateRequest;
import com.bidket.product.presentation.dto.request.product.SkuCreateRequest;
import com.bidket.product.presentation.dto.request.shoesdetail.ProductShoesDetailUpdateRequest;
import com.bidket.product.presentation.dto.response.product.ProductCategoryCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductTypeCreateResponse;
import com.bidket.product.presentation.dto.response.product.SkuCreateResponse;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
public class ProductAdminServiceIntegrationTest {

    @Autowired
    ProductAdminService productAdminService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductCategoryRepository productCategoryRepository;

    @Autowired
    ProductShoesDetailRepository shoesDetailRepository;

    @Autowired
    TestFixture fixture;

    @Autowired
    EntityManager em;

    ProductType pt;
    Brand brand;

    @BeforeEach
    void setup() {
        pt = fixture.createProductType();
        brand = fixture.createBrand();
    }

    @Test
    void createProductType_success() {
        ProductTypeCreateRequest req = new ProductTypeCreateRequest(
                "SHOES",
                "신발",
                "신발 상품 타입"
        );

        ProductTypeCreateResponse res = productAdminService.createProductType(req);

        assertNotNull(res);
    }

    @Test
    void createProduct_fail_whenProductTypeNotExists() {

        //Brand brand = fixture.createBrand();

        ProductCreateRequest req = new ProductCreateRequest(
                UUID.randomUUID(),
                brand.getId(),
                "Nike Dunk Low Retro Panda",
                "나이키 덩크 로우 레트로 팬더",
                "DD1391-100",
                Gender.UNISEX,
                "나이키 덩크 로우 레트로 팬더의 설명",
                LocalDate.of(2025, 1, 1),
                new BigDecimal("199000"),
                ProductStatus.ACTIVE
        );

        assertThrows(ProductException.class,
                () -> productAdminService.createProduct(req));
    }

    @Test
    void createProduct_success() {
        ProductCreateRequest req = new ProductCreateRequest(
                pt.getId(),
                brand.getId(),
                "Nike Dunk Low Retro Panda",
                "나이키 덩크 로우 레트로 팬더",
                "DD1391-100",
                Gender.UNISEX,
                "나이키 덩크 로우 레트로 팬더의 설명",
                LocalDate.of(2025, 1, 1),
                new BigDecimal("199000"),
                ProductStatus.ACTIVE
        );

        ProductCreateResponse res = productAdminService.createProduct(req);

        assertNotNull(res);
    }

    @Test
    void createProductCategory_success() {

        Product product = fixture.createProduct(pt, brand);
        Category category = fixture.createCategory(pt);

        ProductCategoryCreateRequest req = new ProductCategoryCreateRequest(
                category.getId(),
                true
        );

        ProductCategoryCreateResponse res =
                productAdminService.createProductCategory(product.getId(), req);

        assertNotNull(res);
    }

    @Test
    void createSku_success() {

        Product product = fixture.createProduct(pt, brand);
        SizeType st = fixture.createSizeType(pt);
        Size size = fixture.createSize(st);

        SkuCreateRequest req = new SkuCreateRequest(
                size.getId(),
                "DD1391-100-KR",
                SkuStatus.ACTIVE
        );

        SkuCreateResponse res =
                productAdminService.createSku(product.getId() ,req);

        assertNotNull(res);
    }

    @Test
    void createSku_fail_whenDuplicateCode() {

        //ProductType pt = fixture.createProductType();
        //Brand brand = fixture.createBrand();
        Product product = fixture.createProduct(pt, brand);

        SizeType st = fixture.createSizeType(pt);
        Size size = fixture.createSize(st);

        SkuCreateRequest req = new SkuCreateRequest(
                size.getId(),
                "AM97-US10",
                SkuStatus.ACTIVE
        );

        // 첫번째 저장 -> 성공
        productAdminService.createSku(product.getId(),req);

        // 현재 트랜잭션 변경 사항 DB에 반영
        em.flush();

        // 두번째 저장 -> 중복으로 인해 예외 발생
        assertThrows(ProductException.class,
                () -> productAdminService.createSku(product.getId(),req));
    }

    @Test
    void productWithDetailUpdate_product_success() {
        // given
        Product product = fixture.createFullProduct();

        ProductUpdateRequest req = new ProductUpdateRequest(
                null,
                "Updated Product Name",
                "업데이트 상품명",
                null,
                Gender.UNISEX,
                "설명 수정",
                null,
                new BigDecimal("229000"),
                null,
                null,
                null
        );

        // when
        productAdminService.updateProduct(product.getId(), req);

        // then
        Product updated = productRepository.findById(product.getId()).get();

        assertThat(updated.getName()).isEqualTo("Updated Product Name");
        assertThat(updated.getReleasePrice()).isEqualByComparingTo("229000");
    }

    @Test
    void productWithDetailUpdate_productCategory_success() {
        // given
        Product product = fixture.createFullProduct();

        Category newCategory = fixture.createCategory(product.getProductType());

        ProductUpdateRequest req = new ProductUpdateRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(newCategory.getId()),
                newCategory.getId(),
                null
        );

        // when
        productAdminService.updateProduct(product.getId(), req);

        // then
        List<ProductCategory> categories =
                productCategoryRepository.findAllByProduct(product);

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getCategory().getId())
                .isEqualTo(newCategory.getId());
        assertThat(categories.get(0).getIsPrimary()).isTrue();
    }

    @Test
    void productWithDetailUpdate_allInfoUpdate_success() {
        // given
        Product product = fixture.createFullProduct();

        ProductShoesDetailUpdateRequest shoesDetailReq =
                new ProductShoesDetailUpdateRequest(
                        "WHITE/BLACK",
                        "SYNTHETIC",
                        Silhouette.LOW,
                        "SPORT",
                        "AIR",
                        new BigDecimal("850")
                );

        ProductUpdateRequest req = new ProductUpdateRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                shoesDetailReq
        );

        // when
        productAdminService.updateProduct(product.getId(), req);

        // then
        ProductShoesDetail detail =
                shoesDetailRepository.findByProduct_Id(product.getId()).get();

        assertThat(detail.getColorway()).isEqualTo("WHITE/BLACK");
        assertThat(detail.getMainMaterial()).isEqualTo("SYNTHETIC");
        assertThat(detail.getStyle()).isEqualTo("SPORT");
    }
}
