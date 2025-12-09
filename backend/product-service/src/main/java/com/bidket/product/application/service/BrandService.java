package com.bidket.product.application.service;

import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.Brand;
import com.bidket.product.infrastructure.persistence.repository.BrandRepository;
import com.bidket.product.presentation.dto.request.brand.BrandCreateRequest;
import com.bidket.product.presentation.dto.response.brand.BrandCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandCreateResponse createBrand(BrandCreateRequest req) {

        if (brandRepository.findByName(req.name()).isPresent()) {
            throw new ProductException(ProductErrorCode.BRAND_ALREADY_EXISTS);
        }

        Brand brand = Brand.create(
                req.name(),
                req.nameKr(),
                req.originCountry(),
                req.websiteUrl()
        );

        Brand saved = brandRepository.save(brand);
        return BrandCreateResponse.from(saved);
    }
}
