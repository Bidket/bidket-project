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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name ="Product - 조회", description = "상품 조회 API")
public class ProductQueryController {

    private final ProductQueryService productQueryService;
    private final ProductSearchService productSearchService;
    private final ProductPageQueryFacade productPageQueryFacade;

    @Operation(
            summary = "sku 단건 조회",
            description = "활성(ACTIVE) 상태인 sku를 조회합니다."
    )
    @GetMapping("/products/skus/{skuId}")
    public ApiResponse<SkuGetResponse> getSku(
            @PathVariable UUID skuId
    ) {
        return ApiResponse.success(productQueryService.getSku(skuId));
    }

    @Operation(
            summary = "sku 목록 조회",
            description = "sku가 속한 상품의 존재 여부를 확인 후,"
                    + "활성(ACTIVE) 상태인 sku 목록을 페이징 형태로 조회합니다."
    )
    @GetMapping("/products/skus")
    public ApiResponse<PageResponse<SkuGetResponse>> getSkuList(
            SkuGetRequest req,
            PageRequestDto pageRequest
    ) {
        return ApiResponse.success(productQueryService.getSkuList(req, pageRequest));
    }

    @Operation(
            summary = "상품 상세 조회",
            description = "상품과 상세 정보, 상품이 속한 카테고리 목록, 상품의 sku 목록을 모두 조회합니다."
    )
    @GetMapping("/products/{productId}/page")
    public ApiResponse<ProductPageGetResponse> getProductPage(
            @PathVariable UUID productId
    ) {
        return ApiResponse.success(productPageQueryFacade.getPage(productId));
    }

    @Operation(
            summary = "상품 검색 및 필터링",
            description = "키워드(상품명, 상품한글명, 상품 모델코드, 브랜드명, "
                    + "브랜드한글명)로 검색하면 대표카테고리 기반 필터링 되어 "
                    + "페이징 형태로 검색 결과를 조회합니다. "
                    + "상품타입, 브랜드, 성별, 가격 구간 지정으로 필터링 할 수 있습니다."
    )
    @GetMapping("/products/search")
    public ApiResponse<PageResponse<ProductSearchResponse>> getProductSearch(
            ProductSearchRequest req,
            PageRequestDto pageRequest
    ) {
        return ApiResponse.success(productSearchService.search(req, pageRequest));
    }
}
