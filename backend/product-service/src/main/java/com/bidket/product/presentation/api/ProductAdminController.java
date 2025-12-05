package com.bidket.product.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.product.application.service.ProductAdminService;
import com.bidket.product.presentation.dto.request.BrandCreateRequest;
import com.bidket.product.presentation.dto.request.CategoryCreateRequest;
import com.bidket.product.presentation.dto.request.ProductTypeCreateRequest;
import com.bidket.product.presentation.dto.request.SizeCreateRequest;
import com.bidket.product.presentation.dto.request.SizeTypeCreateRequest;
import com.bidket.product.presentation.dto.response.BrandCreateResponse;
import com.bidket.product.presentation.dto.response.CategoryCreateResponse;
import com.bidket.product.presentation.dto.response.ProductTypeCreateResponse;
import com.bidket.product.presentation.dto.response.SizeCreateResponse;
import com.bidket.product.presentation.dto.response.SizeTypeCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductAdminService productAdminService;

    @PostMapping("/product-types")
    public ApiResponse<ProductTypeCreateResponse> createProductType(
            @RequestBody ProductTypeCreateRequest req
    ) {
        return ApiResponse.success(productAdminService.createProductType(req));
    }

    @PostMapping("/brands")
    public ApiResponse<BrandCreateResponse> createBrand(
            @RequestBody BrandCreateRequest req
    ) {
        return ApiResponse.success(productAdminService.createBrand(req));
    }

    @PostMapping("/categories")
    public ApiResponse<CategoryCreateResponse> createCategory(
            @RequestBody CategoryCreateRequest req
    ) {
        return ApiResponse.success(productAdminService.createCategory(req));
    }

    @PostMapping("/size-types")
    public ApiResponse<SizeTypeCreateResponse> createSizeType(
            @RequestBody SizeTypeCreateRequest req
    ) {
        return ApiResponse.success(productAdminService.createSizeType(req));
    }

    @PostMapping("/sizes")
    public ApiResponse<SizeCreateResponse> createSize(
            @RequestBody SizeCreateRequest req
    ) {
        return ApiResponse.success(productAdminService.createSize(req));
    }
}