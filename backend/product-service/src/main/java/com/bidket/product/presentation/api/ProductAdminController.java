package com.bidket.product.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.product.application.facade.ProductAdminFacade;
import com.bidket.product.application.service.BrandService;
import com.bidket.product.application.service.CategoryService;
import com.bidket.product.application.service.ProductAdminService;
import com.bidket.product.application.service.ShoesDetailService;
import com.bidket.product.application.service.SizeService;
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
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class ProductAdminController {

    private final BrandService brandService;
    private final CategoryService categoryService;
    private final SizeService sizeService;
    private final ProductAdminService productAdminService;
    private final ShoesDetailService shoesDetailService;
    private final ProductAdminFacade productAdminFacade;

    @PostMapping("/product-types")
    public ApiResponse<ProductTypeCreateResponse> createProductType(
            @Valid @RequestBody ProductTypeCreateRequest req
    ) {
        return ApiResponse.success(productAdminService.createProductType(req));
    }

    @PostMapping("/brands")
    public ApiResponse<BrandCreateResponse> createBrand(
            @Valid @RequestBody BrandCreateRequest req
    ) {
        return ApiResponse.success(brandService.createBrand(req));
    }

    @PostMapping("/categories")
    public ApiResponse<CategoryCreateResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest req
    ) {
        return ApiResponse.success(categoryService.createCategory(req));
    }

    @PostMapping("/size-types")
    public ApiResponse<SizeTypeCreateResponse> createSizeType(
            @Valid @RequestBody SizeTypeCreateRequest req
    ) {
        return ApiResponse.success(sizeService.createSizeType(req));
    }

    @PostMapping("/sizes")
    public ApiResponse<SizeCreateResponse> createSize(
            @Valid @RequestBody SizeCreateRequest req
    ) {
        return ApiResponse.success(sizeService.createSize(req));
    }

    @PostMapping("/products")
    public ApiResponse<ProductCreateResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest req
    ) {
        return ApiResponse.success(productAdminService.createProduct(req));
    }

    @PostMapping("/products/{productId}/details/shoes")
    public ApiResponse<ProductShoesDetailCreateResponse> createShoesDetail(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductShoesDetailCreateRequest req
    ) {
        return ApiResponse.success(shoesDetailService.createShoesDetail(productId, req));
    }

    @PostMapping("/products/{productId}/categories")
    public ApiResponse<ProductCategoryCreateResponse> createProductCategory(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductCategoryCreateRequest req
    ) {
        return ApiResponse.success(productAdminService.createProductCategory(productId ,req));
    }

    @PostMapping("/products/{productId}/skus")
    public ApiResponse<SkuCreateResponse> createSku(
            @PathVariable UUID productId,
            @Valid @RequestBody SkuCreateRequest req
    ) {
        return ApiResponse.success(productAdminService.createSku(productId, req));
    }

    @PostMapping("/products/with-shoes")
    public ApiResponse<ProductWithShoesCreateResponse> createProductWithShoes(
            @Valid @RequestBody ProductWithShoesCreateRequest req
    ) {
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
            @PathVariable UUID brandId,
            @Valid @RequestBody BrandUpdateRequest req
    ) {
        brandService.updateBrand(brandId, req);
        return ApiResponse.success("브랜드 정보가 수정 되었습니다.", null);
    }

    @Operation(
            summary = "브랜드 상태 수정",
            description = "브랜드의 상태를 ACTIVE 또는 INACTIVE로 수정합니다."
    )
    @PatchMapping("/brands/{brandId}/status")
    public ApiResponse<Void> changeBrandStatus(
            @PathVariable UUID brandId,
            @RequestBody BrandStatusChangeRequest req
    ) {
        brandService.changeBrandStatus(brandId, req.status());
        return ApiResponse.success("브랜드 상태가 수정되었습니다.", null);
    }

    @Operation(
            summary = "카테고리 정보 수정",
            description = "변경이 필요한 카테고리 정보를 부분 수정합니다."
    )
    @PatchMapping("/categories/{categoryId}")
    public ApiResponse<Void> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        categoryService.updateCategory(categoryId, request);
        return ApiResponse.success("카테고리 정보가 수정되었습니다.", null);
    }

    @Operation(
            summary = "카테고리 이동",
            description = "parent 값을 변경해 카테고리를 이동합니다. null이면 최상위 카테고리로 이동합니다."
    )
    @PatchMapping("/categories/{categoryId}/move")
    public ApiResponse<Void> moveCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryMoveRequest request
    ) {
        categoryService.moveCategory(categoryId, request.newParentId());
        return ApiResponse.success("카테고리가 이동되었습니다.", null);
    }

    @Operation(
            summary = "상품 상태 수정",
            description = "상품의 상태를 ACTIVE 또는 INACTIVE로 수정합니다."
    )
    @PatchMapping("/products/{productId}/status")
    public ApiResponse<Void> changeBrandStatus(
            @PathVariable UUID productId,
            @RequestBody ProductStatusChangeRequest req
    ) {
        productAdminService.changeProductStatus(productId, req.status());
        return ApiResponse.success("상품 상태가 수정되었습니다.", null);
    }

    @Operation(
            summary = "상품과 상품 상세 수정",
            description = "상품과 상품 상세 정보를 부분 수정합니다."
    )
    @PatchMapping("/products/{productId}")
    public ApiResponse<Void> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        productAdminService.updateProduct(productId, request);
        return ApiResponse.success("상품 정보가 수정되었습니다.", null);
    }
}