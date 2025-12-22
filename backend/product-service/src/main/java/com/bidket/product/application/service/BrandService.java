package com.bidket.product.application.service;

import com.bidket.product.application.mapper.BrandMapper;
import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.domain.model.BrandStatus;
import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.domain.model.SkuStatus;
import com.bidket.product.infrastructure.persistence.entity.Brand;
import com.bidket.product.infrastructure.persistence.repository.BrandRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductRepository;
import com.bidket.product.infrastructure.persistence.repository.ProductSkuRepository;
import com.bidket.product.presentation.dto.request.brand.BrandCreateRequest;
import com.bidket.product.presentation.dto.request.brand.BrandUpdateRequest;
import com.bidket.product.presentation.dto.response.brand.BrandCreateResponse;
import com.bidket.product.presentation.dto.response.brand.BrandGetResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ProductSkuRepository skuRepository;
    private final BrandMapper brandMapper;

    @Transactional
    public BrandCreateResponse createBrand(BrandCreateRequest req) {

        if (brandRepository.findByName(req.name()).isPresent()) {
            throw new ProductException(ProductErrorCode.BRAND_ALREADY_EXISTS);
        }

        Brand brand = Brand.of(
                req.name(),
                req.nameKr(),
                req.originCountry(),
                req.websiteUrl()
        );

        Brand saved = brandRepository.save(brand);
        return BrandCreateResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<BrandGetResponse> getBrands() {
        return brandRepository.findAllByStatus(BrandStatus.ACTIVE).stream()
                .map(brandMapper::toDto)
                .toList();
    }

    @Transactional
    public void updateBrand(UUID brandId, BrandUpdateRequest req) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.BRAND_NOT_FOUND));

        // name 중복 확인
        if (req.name() != null && !req.name().equals(brand.getName())) {
            if (brandRepository.findByName(req.name()).isPresent()) {
                throw new ProductException(ProductErrorCode.BRAND_ALREADY_EXISTS);
            }
        }

        // nameKr 중복 확인
        String newNameKr = req.nameKr();
        if (newNameKr != null && !newNameKr.isBlank()) {
            String curNameKr = brand.getNameKr();
            boolean changed = (curNameKr == null) || !newNameKr.equals(curNameKr);

            if (changed && brandRepository.findByNameKr(newNameKr).isPresent()) {
                throw new ProductException(ProductErrorCode.BRAND_ALREADY_EXISTS);
            }
        }

        brand.updateInfo(
                req.name(),
                req.nameKr(),
                req.originCountry(),
                req.websiteUrl()
        );
    }

    @Transactional
    public void changeBrandStatus(UUID brandId, BrandStatus status) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.BRAND_NOT_FOUND));

        brand.changeStatus(status);

        if (status == BrandStatus.INACTIVE) {
            // 브랜드 비활성화 -> 해당 브랜드의 상품, sku 비활성화
            productRepository.updateStatusByBrandId(brandId, ProductStatus.INACTIVE);
            skuRepository.updateStatusByBrandId(brandId, SkuStatus.INACTIVE);
        }
    }
}
