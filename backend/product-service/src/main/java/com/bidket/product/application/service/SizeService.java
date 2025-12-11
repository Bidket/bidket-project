package com.bidket.product.application.service;

import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.infrastructure.persistence.entity.Size;
import com.bidket.product.infrastructure.persistence.entity.SizeType;
import com.bidket.product.infrastructure.persistence.repository.ProductTypeRepository;
import com.bidket.product.infrastructure.persistence.repository.SizeRepository;
import com.bidket.product.infrastructure.persistence.repository.SizeTypeRepository;
import com.bidket.product.presentation.dto.request.SizeCreateRequest;
import com.bidket.product.presentation.dto.request.SizeTypeCreateRequest;
import com.bidket.product.presentation.dto.response.SizeCreateResponse;
import com.bidket.product.presentation.dto.response.SizeTypeCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class SizeService {

    private final ProductTypeRepository productTypeRepository;
    private final SizeTypeRepository sizeTypeRepository;
    private final SizeRepository sizeRepository;

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

}
