package com.bidket.product.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.product.application.service.ProductAdminService;
import com.bidket.product.presentation.dto.request.BrandCreateRequest;
import com.bidket.product.presentation.dto.request.CategoryCreateRequest;
import com.bidket.product.presentation.dto.request.ProductCategoryCreateRequest;
import com.bidket.product.presentation.dto.request.ProductCreateRequest;
import com.bidket.product.presentation.dto.request.ProductShoesDetailCreateRequest;
import com.bidket.product.presentation.dto.request.ProductTypeCreateRequest;
import com.bidket.product.presentation.dto.request.SizeCreateRequest;
import com.bidket.product.presentation.dto.request.SizeTypeCreateRequest;
import com.bidket.product.presentation.dto.request.SkuCreateRequest;
import com.bidket.product.presentation.dto.response.BrandCreateResponse;
import com.bidket.product.presentation.dto.response.CategoryCreateResponse;
import com.bidket.product.presentation.dto.response.ProductCategoryCreateResponse;
import com.bidket.product.presentation.dto.response.ProductCreateResponse;
import com.bidket.product.presentation.dto.response.ProductShoesDetailCreateResponse;
import com.bidket.product.presentation.dto.response.ProductTypeCreateResponse;
import com.bidket.product.presentation.dto.response.SizeCreateResponse;
import com.bidket.product.presentation.dto.response.SizeTypeCreateResponse;
import com.bidket.product.presentation.dto.response.SkuCreateResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductAdminService productAdminService;

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
        return ApiResponse.success(productAdminService.createBrand(req));
    }

    @PostMapping("/categories")
    public ApiResponse<CategoryCreateResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest req
    ) {
        return ApiResponse.success(productAdminService.createCategory(req));
    }

    @PostMapping("/size-types")
    public ApiResponse<SizeTypeCreateResponse> createSizeType(
            @Valid @RequestBody SizeTypeCreateRequest req
    ) {
        return ApiResponse.success(productAdminService.createSizeType(req));
    }

    @PostMapping("/sizes")
    public ApiResponse<SizeCreateResponse> createSize(
            @Valid @RequestBody SizeCreateRequest req
    ) {
        return ApiResponse.success(productAdminService.createSize(req));
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
        return ApiResponse.success(productAdminService.createShoesDetail(productId, req));
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
}