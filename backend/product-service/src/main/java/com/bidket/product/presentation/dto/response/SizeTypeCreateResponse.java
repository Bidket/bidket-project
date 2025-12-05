package com.bidket.product.presentation.dto.response;

import com.bidket.product.infrastructure.persistence.entity.SizeType;
import java.util.UUID;

public record SizeTypeCreateResponse(
        UUID id,
        UUID productTypeId,
        String code,
        String regionCode,
        String description,
        Boolean isDefault
) {
    public static SizeTypeCreateResponse from(SizeType sizeType) {
        return new SizeTypeCreateResponse(
                sizeType.getId(),
                sizeType.getProductType().getId(),
                sizeType.getCode(),
                sizeType.getRegionCode(),
                sizeType.getDescription(),
                sizeType.getIsDefault()
        );
    }
}
