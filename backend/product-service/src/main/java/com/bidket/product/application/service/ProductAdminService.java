package com.bidket.product.application.service;

import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.Brand;
import com.bidket.product.infrastructure.persistence.entity.Category;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductCategory;
import com.bidket.product.infrastructure.persistence.entity.ProductShoesDetail;
import com.bidket.product.infrastructure.persistence.entity.ProductSku;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.infrastructure.persistence.entity.Size;
import com.bidket.product.infrastructure.persistence.entity.SizeType;
import com.bidket.product.infrastructure.persistence.repository.BrandRepository;
import com.bidket.product.infrastructure.persistence.repository.CategoryRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductCategoryRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductShoesDetailRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductSkuRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductTypeRepository;
import com.bidket.product.infrastructure.persistence.repository.SizeRepository;
import com.bidket.product.infrastructure.persistence.repository.SizeTypeRepository;
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
    private final SizeTypeRepository sizeTypeRepository;
    private final SizeRepository sizeRepository;
    private final ProductRepository productRepository;
    private final ProductShoesDetailRepository productShoesDetailRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductSkuRepository productSkuRepository;

    public ProductTypeCreateResponse createProductType(ProductTypeCreateRequest req) {

        ProductType productType = ProductType.create(
                req.code(),
                req.name(),
                req.description()
        );

        ProductType saved = productTypeRepository.save(productType);
        return ProductTypeCreateResponse.from(saved);
    }

    public BrandCreateResponse createBrand(BrandCreateRequest req) {

        Brand brand = Brand.create(
                req.name(),
                req.nameKr(),
                req.originCountry(),
                req.websiteUrl()
        );

        Brand saved = brandRepository.save(brand);
        return BrandCreateResponse.from(saved);
    }

    public CategoryCreateResponse createCategory(CategoryCreateRequest req) {

        ProductType productType = productTypeRepository.findById(req.productTypeId())
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_TYPE_NOT_FOUND));

        Category parent = null;
        if (req.parentId() != null) {
            parent = categoryRepository.findById(req.parentId())
                    .orElseThrow(() -> new ProductException(ProductErrorCode.CATEGORY_NOT_FOUND));


            parent.markAsNotLeaf();
        }

        Category category = Category.create(
                productType,
                parent,
                req.name(),
                req.code(),
                req.sortId()
        );

        Category saved = categoryRepository.save(category);
        return CategoryCreateResponse.from(saved);
    }

    public SizeTypeCreateResponse createSizeType(SizeTypeCreateRequest req) {

        ProductType productType = productTypeRepository.findById(req.productTypeId())
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_TYPE_NOT_FOUND));

        /**
         * ProductType별 Default SizeType은 하나만 존재.
         * SizyType을 생성할때 default로 지정하면 기존 default 해제(false 처리).
         */
        boolean isDefault = Boolean.TRUE.equals(req.isDefault());

        if (isDefault) {
            sizeTypeRepository.resetDefault(productType.getId());
        }

        SizeType sizeType = SizeType.create(
                productType,
                req.code(),
                req.regionCode(),
                req.description(),
                isDefault
        );

        SizeType saved = sizeTypeRepository.save(sizeType);
        return SizeTypeCreateResponse.from(saved);
    }

    public SizeCreateResponse createSize(SizeCreateRequest req) {

        SizeType sizeType = sizeTypeRepository.findById(req.sizeTypeId())
                .orElseThrow(() -> new ProductException(ProductErrorCode.SIZE_TYPE_NOT_FOUND));

        Size size = Size.create(
                sizeType,
                req.code(),
                req.displayLabel(),
                req.sortId()
        );

        Size saved = sizeRepository.save(size);
        return SizeCreateResponse.from(saved);
    }

    public ProductCreateResponse createProduct(ProductCreateRequest req) {

        ProductType productType = productTypeRepository.findById(req.productTypeId())
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_TYPE_NOT_FOUND));

        Brand brand = brandRepository.findById(req.brandId())
                .orElseThrow(() -> new ProductException(ProductErrorCode.BRAND_NOT_FOUND));

        Product product = Product.create(
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

    public ProductShoesDetailCreateResponse createShoesDetail(
            UUID productId,
            ProductShoesDetailCreateRequest req
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

        ProductShoesDetail productShoesDetail = ProductShoesDetail.create(
                product,
                req.colorway(),
                req.mainMaterial(),
                req.silhouette(),
                req.style(),
                req.originCountry(),
                req.weight()
        );

        ProductShoesDetail saved = productShoesDetailRepository.save(productShoesDetail);
        return ProductShoesDetailCreateResponse.from(saved);
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

        ProductSku sku = ProductSku.create(
                product,
                size,
                req.skuCode(),
                req.status()
        );

        return SkuCreateResponse.from(productSkuRepository.save(sku));
    }
}
