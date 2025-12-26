package com.bidket.product.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.product.application.facade.ProductAdminFacade;
import com.bidket.product.application.service.BrandService;
import com.bidket.product.application.service.CategoryService;
import com.bidket.product.application.service.ProductAdminService;
import com.bidket.product.application.service.ShoesDetailService;
import com.bidket.product.application.service.SizeService;
import com.bidket.product.application.validator.AdminRoleValidator;
import com.bidket.product.presentation.dto.request.brand.BrandCreateRequest;
import com.bidket.product.presentation.dto.request.brand.BrandStatusChangeRequest;
import com.bidket.product.presentation.dto.request.brand.BrandUpdateRequest;
import com.bidket.product.presentation.dto.request.category.CategoryCreateRequest;
import com.bidket.product.presentation.dto.request.category.CategoryMoveRequest;
import com.bidket.product.presentation.dto.request.category.CategoryUpdateRequest;
import com.bidket.product.presentation.dto.request.product.ProductCategoryCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductStatusChangeRequest;
import com.bidket.product.presentation.dto.request.product.ProductTypeCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductUpdateRequest;
import com.bidket.product.presentation.dto.request.product.ProductWithShoesCreateRequest;
import com.bidket.product.presentation.dto.request.product.SkuCreateRequest;
import com.bidket.product.presentation.dto.request.product.SkuStatusChangeRequest;
import com.bidket.product.presentation.dto.request.shoesdetail.ProductShoesDetailCreateRequest;
import com.bidket.product.presentation.dto.request.size.SizeCreateRequest;
import com.bidket.product.presentation.dto.request.size.SizeTypeCreateRequest;
import com.bidket.product.presentation.dto.response.brand.BrandCreateResponse;
import com.bidket.product.presentation.dto.response.category.CategoryCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductCategoryCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductTypeCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductWithShoesCreateResponse;
import com.bidket.product.presentation.dto.response.product.SkuCreateResponse;
import com.bidket.product.presentation.dto.response.shoesdetail.ProductShoesDetailCreateResponse;
import com.bidket.product.presentation.dto.response.size.SizeCreateResponse;
import com.bidket.product.presentation.dto.response.size.SizeTypeCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Product - 어드민 생성, 수정", description = "어드민 상품 생성, 수정 API")
public class ProductAdminController {

    private final BrandService brandService;
    private final CategoryService categoryService;
    private final SizeService sizeService;
    private final ProductAdminService productAdminService;
    private final ShoesDetailService shoesDetailService;
    private final ProductAdminFacade productAdminFacade;
    private final AdminRoleValidator adminRoleValidator;

    @Operation(
            summary = "상품 타입 생성",
            description = "상품 타입을 생성합니다."
    )
    @PostMapping("/product-types")
    public ApiResponse<ProductTypeCreateResponse> createProductType(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody ProductTypeCreateRequest req
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(productAdminService.createProductType(req));
    }

    @Operation(
            summary = "브랜드 생성",
            description = "브랜드를 생성합니다."
    )
    @PostMapping("/brands")
    public ApiResponse<BrandCreateResponse> createBrand(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody BrandCreateRequest req
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(brandService.createBrand(req));
    }

    @Operation(
            summary = "카테고리 생성",
            description = "카테고리를 생성합니다. 상위 카테고리 정보가 있다면, "
                    + "상위 카테고리가 말단 카테고리가 아님을 표시합니다."
    )
    @PostMapping("/categories")
    public ApiResponse<CategoryCreateResponse> createCategory(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CategoryCreateRequest req
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(categoryService.createCategory(req));
    }

    @Operation(
            summary = "사이즈타입 생성",
            description = "사이즈타입을 생성합니다. 디폴트값이라면 "
                    + "해당 상품 타입의 기존 디폴트를 해제 합니다."
    )
    @PostMapping("/size-types")
    public ApiResponse<SizeTypeCreateResponse> createSizeType(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody SizeTypeCreateRequest req
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(sizeService.createSizeType(req));
    }

    @Operation(
            summary = "사이즈 생성",
            description = "사이즈를 생성합니다."
    )
    @PostMapping("/sizes")
    public ApiResponse<SizeCreateResponse> createSize(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody SizeCreateRequest req
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(sizeService.createSize(req));
    }

    @Operation(
            summary = "상품 생성",
            description = "상품을 생성합니다."
    )
    @PostMapping("/products")
    public ApiResponse<ProductCreateResponse> createProduct(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody ProductCreateRequest req
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(productAdminService.createProduct(req));
    }

    @Operation(
            summary = "신발 상품 상세 생성",
            description = "신발 상품 상세를 생성합니다."
    )
    @PostMapping("/products/{productId}/details/shoes")
    public ApiResponse<ProductShoesDetailCreateResponse> createShoesDetail(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID productId,
            @Valid @RequestBody ProductShoesDetailCreateRequest req
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(shoesDetailService.createShoesDetail(productId, req));
    }

    @Operation(
            summary = "상품과 카테고리 매핑",
            description = "상품과 카테고리 매핑을 생성합니다."
    )
    @PostMapping("/products/{productId}/categories")
    public ApiResponse<ProductCategoryCreateResponse> createProductCategory(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID productId,
            @Valid @RequestBody ProductCategoryCreateRequest req
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(productAdminService.createProductCategory(productId ,req));
    }

    @Operation(
            summary = "SKU 생성",
            description = "SKU를 생성합니다."
    )
    @PostMapping("/products/{productId}/skus")
    public ApiResponse<SkuCreateResponse> createSku(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID productId,
            @Valid @RequestBody SkuCreateRequest req
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(productAdminService.createSku(productId, req));
    }

    @Operation(
            summary = "상품 + 신발 상품 상세 생성",
            description = "상품과 신발 상품 상세를 함께 생성합니다."
    )
    @PostMapping("/products/with-shoes")
    public ApiResponse<ProductWithShoesCreateResponse> createProductWithShoes(
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody ProductWithShoesCreateRequest req
    ) {
        adminRoleValidator.validate(role);
        return ApiResponse.success(
                productAdminFacade.createProductWithShoes(req)
        );
    }
    @Operation(
            summary = "브랜드 수정",
            description = "변경이 필요한 브랜드 정보를 부분 수정합니다."
    )
    @PatchMapping("/brands/{brandId}")
    public ApiResponse<Void> updateBrand(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID brandId,
            @Valid @RequestBody BrandUpdateRequest req
    ) {
        adminRoleValidator.validate(role);
        brandService.updateBrand(brandId, req);
        return ApiResponse.success("브랜드 정보가 수정 되었습니다.", null);
    }

    @Operation(
            summary = "브랜드 상태 수정",
            description = "브랜드의 상태를 ACTIVE 또는 INACTIVE로 수정합니다."
    )
    @PatchMapping("/brands/{brandId}/status")
    public ApiResponse<Void> changeBrandStatus(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID brandId,
            @RequestBody BrandStatusChangeRequest req
    ) {
        adminRoleValidator.validate(role);
        brandService.changeBrandStatus(brandId, req.status());
        return ApiResponse.success("브랜드 상태가 수정되었습니다.", null);
    }

    @Operation(
            summary = "카테고리 정보 수정",
            description = "변경이 필요한 카테고리 정보를 부분 수정합니다."
    )
    @PatchMapping("/categories/{categoryId}")
    public ApiResponse<Void> updateCategory(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        adminRoleValidator.validate(role);
        categoryService.updateCategory(categoryId, request);
        return ApiResponse.success("카테고리 정보가 수정되었습니다.", null);
    }

    @Operation(
            summary = "카테고리 이동",
            description = "parent 값을 변경해 카테고리를 이동합니다. null이면 최상위 카테고리로 이동합니다."
    )
    @PatchMapping("/categories/{categoryId}/move")
    public ApiResponse<Void> moveCategory(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryMoveRequest request
    ) {
        adminRoleValidator.validate(role);
        categoryService.moveCategory(categoryId, request.newParentId());
        return ApiResponse.success("카테고리가 이동되었습니다.", null);
    }

    @Operation(
            summary = "상품 상태 수정",
            description = "상품의 상태를 ACTIVE 또는 INACTIVE로 수정합니다."
    )
    @PatchMapping("/products/{productId}/status")
    public ApiResponse<Void> changeBrandStatus(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID productId,
            @RequestBody ProductStatusChangeRequest req
    ) {
        adminRoleValidator.validate(role);
        productAdminService.changeProductStatus(productId, req.status());
        return ApiResponse.success("상품 상태가 수정되었습니다.", null);
    }

    @Operation(
            summary = "상품과 상품 상세 수정",
            description = "상품과 상품 상세 정보를 부분 수정합니다."
    )
    @PatchMapping("/products/{productId}")
    public ApiResponse<Void> updateProduct(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        adminRoleValidator.validate(role);
        productAdminService.updateProduct(productId, request);
        return ApiResponse.success("상품 정보가 수정되었습니다.", null);
    }

    @Operation(
            summary = "SKU 상태 수정",
            description = "SKU의 상태를 ACTIVE 또는 INACTIVE로 수정합니다."
    )
    @PatchMapping("/products/{productId}/skus/{skuId}/status")
    public ApiResponse<Void> changeSkuStatus(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID productId,
            @PathVariable UUID skuId,
            @Valid @RequestBody SkuStatusChangeRequest req
    ) {
        adminRoleValidator.validate(role);
        productAdminService.changeSkuStatus(productId, skuId, req.status());
        return ApiResponse.success("SKU 상태가 수정되었습니다.", null);
    }
}