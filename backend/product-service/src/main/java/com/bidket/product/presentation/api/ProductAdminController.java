package com.bidket.product.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.product.application.facade.ProductAdminFacade;
import com.bidket.product.application.service.BrandService;
import com.bidket.product.application.service.CategoryService;
import com.bidket.product.application.service.ProductAdminService;
import com.bidket.product.application.service.ShoesDetailService;
import com.bidket.product.application.service.SizeService;
import com.bidket.product.presentation.dto.request.brand.BrandCreateRequest;
import com.bidket.product.presentation.dto.request.category.CategoryCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductCategoryCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductCreateRequest;
import com.bidket.product.presentation.dto.request.shoesdetail.ProductShoesDetailCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductTypeCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductWithShoesCreateRequest;
import com.bidket.product.presentation.dto.request.size.SizeCreateRequest;
import com.bidket.product.presentation.dto.request.size.SizeTypeCreateRequest;
import com.bidket.product.presentation.dto.request.product.SkuCreateRequest;
import com.bidket.product.presentation.dto.response.brand.BrandCreateResponse;
import com.bidket.product.presentation.dto.response.category.CategoryCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductCategoryCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductCreateResponse;
import com.bidket.product.presentation.dto.response.shoesdetail.ProductShoesDetailCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductTypeCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductWithShoesCreateResponse;
import com.bidket.product.presentation.dto.response.size.SizeCreateResponse;
import com.bidket.product.presentation.dto.response.size.SizeTypeCreateResponse;
import com.bidket.product.presentation.dto.response.product.SkuCreateResponse;
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
}