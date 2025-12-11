package com.bidket.product.application.service;

import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.Brand;
import com.bidket.product.infrastructure.persistence.entity.Category;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductCategory;
import com.bidket.product.infrastructure.persistence.entity.ProductSku;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.infrastructure.persistence.entity.Size;
import com.bidket.product.infrastructure.persistence.repository.BrandRepository;
import com.bidket.product.infrastructure.persistence.repository.CategoryRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductCategoryRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductSkuRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductTypeRepository;
import com.bidket.product.infrastructure.persistence.repository.SizeRepository;
import com.bidket.product.presentation.dto.request.product.ProductCategoryCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductCreateRequest;
import com.bidket.product.presentation.dto.request.product.ProductTypeCreateRequest;
import com.bidket.product.presentation.dto.request.product.SkuCreateRequest;
import com.bidket.product.presentation.dto.response.product.ProductCategoryCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductTypeCreateResponse;
import com.bidket.product.presentation.dto.response.product.SkuCreateResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductAdminService {

    private final ProductTypeRepository productTypeRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductSkuRepository productSkuRepository;

    public ProductTypeCreateResponse createProductType(ProductTypeCreateRequest req) {

        ProductType productType = ProductType.of(
                req.code(),
                req.name(),
                req.description()
        );

        ProductType saved = productTypeRepository.save(productType);
        return ProductTypeCreateResponse.from(saved);
    }

    public ProductCreateResponse createProduct(ProductCreateRequest req) {

        ProductType productType = productTypeRepository.findById(req.productTypeId())
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_TYPE_NOT_FOUND));

        Brand brand = brandRepository.findById(req.brandId())
                .orElseThrow(() -> new ProductException(ProductErrorCode.BRAND_NOT_FOUND));

        Product product = Product.of(
                productType,
                brand,
                req.name(),
                req.nameKr(),
                req.modelCode(),
                req.gender(),
                req.description(),
                req.releaseDate(),
                req.releasePrice(),
                req.status()
        );

        Product saved = productRepository.save(product);
        return ProductCreateResponse.from(saved);
    }

    public ProductCategoryCreateResponse createProductCategory(
            UUID productId,
            ProductCategoryCreateRequest req
    ) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND));

        boolean isPrimary = Boolean.TRUE.equals(req.isPrimary());

        if (isPrimary) {
            productCategoryRepository.resetPrimary(productId);
        }

        ProductCategory productCategory = ProductCategory.create(product, category, isPrimary);


        return ProductCategoryCreateResponse.from(productCategoryRepository.save(productCategory));
    }

    public SkuCreateResponse createSku(
            UUID productId,
            SkuCreateRequest req
    ) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

        Size size = sizeRepository.findById(req.sizeId())
                .orElseThrow(() -> new ProductException(ProductErrorCode.SIZE_NOT_FOUND));

        if (productSkuRepository.existsBySkuCode(req.skuCode())) {
            throw new ProductException(ProductErrorCode.SKU_CODE_ALREADY_EXISTS);
        }

        ProductSku sku = ProductSku.of(
                product,
                size,
                req.skuCode(),
                req.status()
        );

        return SkuCreateResponse.from(productSkuRepository.save(sku));
    }
}
