package com.bidket.product.common;

import com.bidket.product.domain.model.Gender;
import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.infrastructure.persistence.entity.Brand;
import com.bidket.product.infrastructure.persistence.entity.Category;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.infrastructure.persistence.entity.Size;
import com.bidket.product.infrastructure.persistence.entity.SizeType;
import com.bidket.product.infrastructure.persistence.repository.BrandRepository;
import com.bidket.product.infrastructure.persistence.repository.CategoryRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductTypeRepository;
import com.bidket.product.infrastructure.persistence.repository.SizeRepository;
import com.bidket.product.infrastructure.persistence.repository.SizeTypeRepository;
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

    public ProductType createProductType() {
        ProductType type = ProductType.create(
                "SHOES",
                "신발",
                "신발 상품 타입"
        );
        return productTypeRepository.save(type);
    }

    public Brand createBrand() {
        Brand brand = Brand.create(
                "Nike",
                "나이키",
                "US",
                "https://nike.com"
                //BrandStatus.ACTIVE
        );
        return brandRepository.save(brand);
    }

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

    public SizeType createSizeType(ProductType pt) {
        SizeType st = SizeType.create(
                pt,
                "SHOES_KR_MM",
                "KR",
                "사이즈타입 KR 설명",
                true
        );
        return sizeTypeRepository.save(st);
    }

    public Size createSize(SizeType st) {
        Size size = Size.create(
                st,
                "260",
                "260mm",
                100L
        );
        return sizeRepository.save(size);
    }

    public Product createProduct(ProductType pt, Brand brand) {
        Product p = Product.create(
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
}
