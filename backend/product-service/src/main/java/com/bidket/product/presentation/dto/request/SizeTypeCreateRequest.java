package com.bidket.product.presentation.dto.request;

import java.util.UUID;

public record SizeTypeCreateRequest(
        UUID productTypeId,
        String code,
        String regionCode,
        String description,
        Boolean isDefault
) {}
