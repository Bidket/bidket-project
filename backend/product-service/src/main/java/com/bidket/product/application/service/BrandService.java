package com.bidket.product.application.service;

import com.bidket.product.infrastructure.persistence.entity.Brand;
import com.bidket.product.infrastructure.persistence.repository.BrandRepository;
import com.bidket.product.presentation.dto.request.BrandCreateRequest;
import com.bidket.product.presentation.dto.response.BrandCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

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
}
