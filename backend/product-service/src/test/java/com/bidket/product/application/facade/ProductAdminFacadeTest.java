package com.bidket.product.application.facade;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.bidket.product.application.service.ProductAdminService;
import com.bidket.product.application.service.ShoesDetailService;
import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.domain.model.Gender;
import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.domain.model.Silhouette;
import com.bidket.product.presentation.dto.request.product.ProductCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductWithShoesCreateRequest;
import com.bidket.product.presentation.dto.request.shoesdetail.ProductShoesDetailCreateRequest;
import com.bidket.product.presentation.dto.response.product.ProductCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductWithShoesCreateResponse;
import com.bidket.product.presentation.dto.response.shoesdetail.ProductShoesDetailCreateResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductAdminFacadeTest {

    @InjectMocks
    ProductAdminFacade productAdminFacade;

    @Mock
    ProductAdminService productAdminService;

    @Mock
    ShoesDetailService shoesDetailService;

    @Test
    @DisplayName("상품 + 신발 상품 생성 성공")
    void createProductWithShoes_success() {

        // given
        UUID productId = UUID.randomUUID();
        UUID shoesDetailId = UUID.randomUUID();

        // 1. product req 생성
        ProductCreateRequest productReq = new ProductCreateRequest(
                UUID.randomUUID(), // productTypeId
                UUID.randomUUID(), // brandId
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
                        null, // facade에서 생성
                        "Triple White",
                        "Leather",
                        Silhouette.LOW,
                        "Classic",
                        "Vietnam",
                        new BigDecimal("350")
                );

        ProductWithShoesCreateRequest bundleReq =
                new ProductWithShoesCreateRequest(productReq, shoesReq);

        // mock 설정
        when(productAdminService.createProduct(any(ProductCreateRequest.class)))
                .thenReturn(new ProductCreateResponse(
                        productId,
                        "Nike Air Max 97",
                        "AM97",
                        Gender.UNISEX,
                        LocalDate.of(2025, 1, 1),
                        new BigDecimal("199000"),
                        "설명",
                        ProductStatus.ACTIVE
                ));

        when(shoesDetailService.createShoesDetail(any(), any()))
                .thenReturn(new ProductShoesDetailCreateResponse(
                        shoesDetailId,
                        productId,
                        "Triple White",
                        "Leather",
                        Silhouette.LOW,
                        "Classic",
                        "Vietnam",
                        new BigDecimal("350")
                ));

        // when
        ProductWithShoesCreateResponse response =
                productAdminFacade.createProductWithShoes(bundleReq);

        // then
        // 응답 검증
        assertNotNull(response);

        //product
        assertNotNull(response.product());
        assertEquals(productId, response.product().id());
        assertEquals("Nike Air Max 97", response.product().name());
        assertEquals("AM97", response.product().modelCode());

        // shoesDetail
        assertNotNull(response.shoesDetail());
        assertEquals(shoesDetailId, response.shoesDetail().id());
        assertEquals(productId, response.shoesDetail().productId());

        // 내부 서비스 호출 검증
        verify(productAdminService, times(1)).createProduct(any());
        verify(shoesDetailService, times(1)).createShoesDetail(any(), any());
    }

    @Test
    @DisplayName("상품 + 신발 상품 생성 실패 - productTypeId 없음")
    void createProductWithShoes_fail_whenProductTypeNotFound() {

        // given
        UUID invalidProductTypeId = UUID.randomUUID();
        UUID validBrandId = UUID.randomUUID();

        ProductCreateRequest productReq = new ProductCreateRequest(
                invalidProductTypeId,
                validBrandId,
                "Nike Air Max 97",
                "나이키 에어 맥스 97",
                "AM97",
                Gender.UNISEX,
                "설명",
                LocalDate.now(),
                new BigDecimal("199000"),
                ProductStatus.ACTIVE
        );

        ProductShoesDetailCreateRequest shoesReq =
                new ProductShoesDetailCreateRequest(
                        null,
                        "Triple White",
                        "Leather",
                        Silhouette.LOW,
                        "Classic",
                        "Vietnam",
                        new BigDecimal("350")
                );

        ProductWithShoesCreateRequest bundle =
                new ProductWithShoesCreateRequest(productReq, shoesReq);

        // mock: ProductAdminService가 예외를 던지도록 설정
        when(productAdminService.createProduct(any(ProductCreateRequest.class)))
                .thenThrow(new ProductException(ProductErrorCode.PRODUCT_TYPE_NOT_FOUND));

        // when & then
        ProductException ex = assertThrows(
                ProductException.class,
                () -> productAdminFacade.createProductWithShoes(bundle)
        );

        assertEquals(ProductErrorCode.PRODUCT_TYPE_NOT_FOUND, ex.getErrorCode());
    }
}