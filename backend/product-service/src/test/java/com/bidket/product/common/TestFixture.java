package com.bidket.product.common;

import com.bidket.product.domain.model.Gender;
import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.domain.model.Silhouette;
import com.bidket.product.domain.model.SkuStatus;
import com.bidket.product.infrastructure.persistence.entity.*;
import com.bidket.product.infrastructure.persistence.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestFixture {

    private final ProductTypeRepository productTypeRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final SizeTypeRepository sizeTypeRepository;
    private final SizeRepository sizeRepository;
    private final ProductRepository productRepository;
    private final ProductShoesDetailRepository shoesDetailRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductSkuRepository skuRepository;

    // 상품타입 생성
    public ProductType createProductType() {
        ProductType type = ProductType.of(
                "SHOES",
                "신발",
                "신발 상품 타입"
        );
        return productTypeRepository.save(type);
    }

    // 브랜드 생성
    public Brand createBrand() {
        Brand brand = Brand.of(
                "Nike",
                "나이키",
                "US",
                "https://nike.com"
                //BrandStatus.ACTIVE
        );
        return brandRepository.save(brand);
    }

    // 카테고리 생성
    public Category createCategory(ProductType productType) {
        Category category = Category.create(
                productType,
                null,
                "조던",
                "JORDAN",
                100L
        );
        return categoryRepository.save(category);
    }

    // 사이즈타입 생성
    public SizeType createSizeType(ProductType pt) {
        SizeType st = SizeType.of(
                pt,
                "SHOES_KR_MM",
                "KR",
                "사이즈타입 KR 설명",
                true
        );
        return sizeTypeRepository.save(st);
    }

    // 사이즈 생성
    public Size createSize(SizeType st) {
        Size size = Size.of(
                st,
                "260",
                "260mm",
                100L
        );
        return sizeRepository.save(size);
    }

    // 상품 생성
    public Product createProduct(ProductType pt, Brand brand) {
        Product p = Product.of(
                pt,
                brand,
                "Nike Dunk Low Retro Panda",
                "나이키 덩크 로우 레트로 팬더",
                "DD1391-100",
                Gender.UNISEX,
                "나이키 덩크 로우 레트로 팬더의 설명",
                LocalDate.of(2025, 1, 1),
                new BigDecimal("199000"),
                ProductStatus.ACTIVE
        );
        return productRepository.save(p);
    }

    // 상품-카테고리 매핑 생성
    public ProductCategory createProductCategory(Product product, Category category, boolean isPrimary) {
        ProductCategory pc = ProductCategory.create(product, category, isPrimary);
        return productCategoryRepository.save(pc);
    }

    // 신발 상품 상세 정보 생성
    public ProductShoesDetail createShoesDetail(Product product) {
        ProductShoesDetail detail = ProductShoesDetail.of(
                product,
                "BLACK/WHITE",
                "LEATHER",
                Silhouette.LOW,
                "CASUAL",
                "VM",
                new BigDecimal(800)
        );
        return shoesDetailRepository.save(detail);
    }

    // SKU 생성
    public ProductSku createSku(Product product, Size size, String skuCode) {
        ProductSku sku = ProductSku.of(
                product,
                size,
                skuCode,
                SkuStatus.ACTIVE
        );
        return skuRepository.save(sku);
    }

    // 상품 + 카테고리 + 신발 상품 상세 정보 + 사이즈 + SKU 생성
    public Product createFullProduct() {
        ProductType pt = createProductType();

        Brand brand = createBrand();
        Product product = createProduct(pt, brand);

        // Category
        Category category = createCategory(pt);
        createProductCategory(product, category, true);

        // SizeType + Size
        SizeType st = createSizeType(pt);
        Size size = createSize(st);

        // Shoes Detail
        createShoesDetail(product);

        // SKU
        createSku(product, size, "DD1391-100-260");

        return product;
    }
}
