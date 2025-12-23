package com.bidket.product.application.facade;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidket.product.common.TestFixture;
import com.bidket.product.infrastructure.persistence.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProductDetailQueryFacadeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestFixture testFixture;

    @Test
    @DisplayName("퍼블릭 상품 상세 조회 성공 - 신발 상품")
    void getProductDetail_success() throws Exception {
        // given
        Product product = testFixture.createFullProduct();

        // when & then
        mockMvc.perform(
                        get("/v1/products/{productId}", product.getId())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))

                // 상품 기본 정보
                .andExpect(jsonPath("$.data.id").value(product.getId().toString()))
                .andExpect(jsonPath("$.data.name")
                        .value("Nike Dunk Low Retro Panda"))
                .andExpect(jsonPath("$.data.brandName").value("Nike"))
                .andExpect(jsonPath("$.data.productTypeName").value("신발"))

                // 카테고리 (대표 카테고리)
                .andExpect(jsonPath("$.data.categories").isArray())
                .andExpect(jsonPath("$.data.categories[0].name").value("조던"))

                // SKU
                .andExpect(jsonPath("$.data.skus").isArray())
                .andExpect(jsonPath("$.data.skus[0].skuCode")
                        .value("DD1391-100-260"))

                // ShoesDetail (Resolver 검증)
                .andExpect(jsonPath("$.data.shoesDetail").isNotEmpty())
                .andExpect(jsonPath("$.data.shoesDetail.colorway")
                        .value("BLACK/WHITE"))
                .andExpect(jsonPath("$.data.shoesDetail.silhouette")
                        .value("LOW"));
    }

    @Test
    @DisplayName("퍼블릭 상품 상세 조회 실패 - INACTIVE 상품")
    void getProductDetail_inactiveProduct() throws Exception {
        // given
        Product inactiveProduct = testFixture.createInactiveProduct();

        // when & then
        mockMvc.perform(
                        get("/v1/products/{productId}", inactiveProduct.getId())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());
    }
}
