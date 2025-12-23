package com.bidket.product.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.common.presentation.response.PageResponse;
import com.bidket.product.application.facade.AdminProductDetailFacade;
import com.bidket.product.application.service.AdminQueryService;
import com.bidket.product.application.validator.AdminRoleValidator;
import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.presentation.dto.request.PageRequestDto;
import com.bidket.product.presentation.dto.response.product.ProductGetAdminDetailResponse;
import com.bidket.product.presentation.dto.response.product.ProductGetAdminResponse;
import com.bidket.product.presentation.dto.response.product.ProductGetAdminSimpleResponse;
import com.bidket.product.presentation.dto.response.product.SkuGetAdminResponse;
import com.bidket.product.presentation.dto.response.size.SizeGetAdminResponse;
import com.bidket.product.presentation.dto.response.size.SizeTypeGetAdminResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Product - 어드민 조회", description = "어드민 상품 조회 API")
public class AdminQueryController {

    private final AdminQueryService adminQueryService;
    private final AdminProductDetailFacade detailFacade;
    private final AdminRoleValidator adminRoleValidator;

    @Operation(
            summary = "어드민 사이즈 타입 목록 조회",
            description = "어드민이 사이즈 타입 목록을 조회합니다. 특정 상품 타입 ID로 필터링할 수 있습니다."
    )
    @GetMapping("/size-types")
    public ApiResponse<PageResponse<SizeTypeGetAdminResponse>> getSizeTypes(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) UUID productTypeId,
            PageRequestDto pageRequest
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(adminQueryService.getSizeTypes(productTypeId, pageRequest));
    }

    @Operation(
            summary = "어드민 사이즈 목록 조회",
            description = "어드민이 사이즈 목록을 조회합니다. 특정 사이즈 타입 ID로 필터링할 수 있습니다."
    )
    @GetMapping("/sizes")
    public ApiResponse<PageResponse<SizeGetAdminResponse>> getSizes(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) UUID sizeTypeId,
            PageRequestDto pageRequest
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(adminQueryService.getSizes(sizeTypeId, pageRequest));
    }

    @Operation(
            summary = "어드민 상품 목록 조회",
            description = "어드민이 모든 상태의 상품 목록을 조회합니다. "
                    + "상태값, 특정 상품 타입ID, 특정 브랜드 ID로 필터링할 수 있습니다."
    )
    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductGetAdminResponse>> getProducts(
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "ALL") ProductStatus status,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) UUID productTypeId,
            PageRequestDto pageRequest
    ) {
        adminRoleValidator.validate(role);
        log.info("/v1/admin/products Received status: {}", status);
        return ApiResponse.success(
                adminQueryService.getProducts(
                        status,
                        brandId,
                        productTypeId,
                        pageRequest
                )
        );
    }

    @Operation(
            summary = "어드민 상품 단건 조회",
            description = "어드민이 상품 단건을 조회합니다. "
    )
    @GetMapping("/products/{productId}")
    public ApiResponse<ProductGetAdminSimpleResponse> getProduct(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID productId
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(adminQueryService.getProduct(productId));
    }

    @Operation(
            summary = "어드민 sku 목록 조회",
            description = "어드민이 특정 상품에 속한 sku 목록을 조회합니다. "
    )
    @GetMapping("/products/{productId}/skus")
    public ApiResponse<PageResponse<SkuGetAdminResponse>> getProductSkus(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID productId,
            PageRequestDto pageRequest
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(
                adminQueryService.getProductSkus(productId, pageRequest)
        );
    }

    @Operation(
            summary = "어드민 상품 상세 페이지 조회",
            description = "어드민이 특정 상품의 모든 상세 정보를 조회합니다. "
    )
    @GetMapping("/products/{productId}/detail")
    public ApiResponse<ProductGetAdminDetailResponse> getProductDetail(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID productId
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(
                detailFacade.getProductDetail(productId)
        );
    }
}
