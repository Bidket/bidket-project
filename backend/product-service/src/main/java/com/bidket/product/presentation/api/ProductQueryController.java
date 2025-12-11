package com.bidket.product.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.common.presentation.response.PageResponse;
import com.bidket.product.application.facade.ProductPageQueryFacade;
import com.bidket.product.application.service.ProductQueryService;
import com.bidket.product.application.service.ProductSearchService;
import com.bidket.product.presentation.dto.request.PageRequestDto;
import com.bidket.product.presentation.dto.request.product.ProductSearchRequest;
import com.bidket.product.presentation.dto.request.product.SkuGetRequest;
import com.bidket.product.presentation.dto.response.product.ProductPageGetResponse;
import com.bidket.product.presentation.dto.response.product.ProductSearchResponse;
import com.bidket.product.presentation.dto.response.product.SkuGetResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ProductQueryController {

    private final ProductQueryService productQueryService;
    private final ProductSearchService productSearchService;
    private final ProductPageQueryFacade productPageQueryFacade;

    @GetMapping("/products/skus/{skuId}")
    public ApiResponse<SkuGetResponse> getSku(
            @PathVariable UUID skuId
    ) {
        return ApiResponse.success(productQueryService.getSku(skuId));
    }

    @GetMapping("/products/skus")
    public ApiResponse<PageResponse<SkuGetResponse>> getSkuList(
            SkuGetRequest req,
            PageRequestDto pageRequest
    ) {
        return ApiResponse.success(productQueryService.getSkuList(req, pageRequest));
    }

    @GetMapping("/products/{productId}/page")
    public ApiResponse<ProductPageGetResponse> getProductPage(
            @PathVariable UUID productId
    ) {
        return ApiResponse.success(productPageQueryFacade.getPage(productId));
    }

    @GetMapping("/products/search")
    public ApiResponse<PageResponse<ProductSearchResponse>> getProductSearch(
            ProductSearchRequest req,
            PageRequestDto pageRequest
    ) {
        return ApiResponse.success(productSearchService.search(req, pageRequest));
    }
}
