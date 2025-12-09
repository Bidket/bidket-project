package com.bidket.product.presentation.dto.response;

import com.bidket.product.infrastructure.persistence.entity.Size;
import java.util.UUID;

public record SizeCreateResponse(
        UUID id,
        UUID sizeTypeId,
        String code,
        String displayLabel,
        Long sortId
) {
    public static SizeCreateResponse from(Size size) {
        return new SizeCreateResponse(
                size.getId(),
                size.getSizeType().getId(),
                size.getCode(),
                size.getDisplayLabel(),
                size.getSortId()
        );
    }
}
