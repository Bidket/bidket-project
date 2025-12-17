package com.bidket.product.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidket.common.presentation.response.PageResponse;
import com.bidket.product.application.facade.ProductPageQueryFacade;
import com.bidket.product.common.TestFixture;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductSku;
import com.bidket.product.infrastructure.persistence.repository.ProductSkuRepository;
import com.bidket.product.presentation.dto.request.PageRequestDto;
import com.bidket.product.presentation.dto.request.product.ProductSearchRequest;
import com.bidket.product.presentation.dto.request.product.SkuGetRequest;
import com.bidket.product.presentation.dto.response.product.ProductPageGetResponse;
import com.bidket.product.presentation.dto.response.product.ProductSearchResponse;
import com.bidket.product.presentation.dto.response.product.SkuGetDetailResponse;
import com.bidket.product.presentation.dto.response.product.SkuGetResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ProductQueryServiceIntegrationTest {

    @Autowired
    TestFixture fixture;

    @Autowired
    ProductQueryService productQueryService;

    @Autowired
    ProductPageQueryFacade productPageQueryFacade;

    @Autowired
    ProductSearchService productSearchService;

    @Autowired
    ProductSkuRepository skuRepository;

    private Product setupProduct() {
        return fixture.createFullProduct();
    }

    @Test
    void getSku_success() {
        Product product = setupProduct();
        ProductSku sku = skuRepository.findAll().get(0);

        SkuGetDetailResponse result = productQueryService.getSku(sku.getId());

        assertThat(result.skuCode()).isEqualTo("DD1391-100-260");
    }

    @Test
    void getSkuList_success() {
        Product product = setupProduct();
        SkuGetRequest req = new SkuGetRequest(product.getId(), null, null);
        PageRequestDto pageReq = new PageRequestDto(0, 10, "createdAt");

        PageResponse<SkuGetResponse> result =
                productQueryService.getSkuList(req, pageReq);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).skuCode()).isEqualTo("DD1391-100-260");
    }

    @Test
    void getPage_success() {
        Product product = setupProduct();

        ProductPageGetResponse response =
                productPageQueryFacade.getPage(product.getId());

        assertThat(response.name()).isEqualTo("Nike Dunk Low Retro Panda");
        assertThat(response.shoesDetail().colorway()).isEqualTo("BLACK/WHITE");
        assertThat(response.categories().get(0).name()).isEqualTo("조던");
        assertThat(response.skus().get(0).skuCode()).isEqualTo("DD1391-100-260");
    }

    @Test
    void search_success() {
        Product product = setupProduct();
        ProductSearchRequest req = new ProductSearchRequest(
                "Dunk", null, null,
                null, null, null, null
        );
        PageRequestDto pageReq = new PageRequestDto(0, 10, "createdAt");

        PageResponse<ProductSearchResponse> result =
                productSearchService.search(req, pageReq);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).name())
                .isEqualTo("Nike Dunk Low Retro Panda");
    }
}