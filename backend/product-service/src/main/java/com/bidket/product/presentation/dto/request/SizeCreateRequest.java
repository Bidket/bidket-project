package com.bidket.product.presentation.dto.request;

import java.util.UUID;

public record SizeCreateRequest(
        UUID sizeTypeId,
        String code,
        String displayLabel,
        Long sortId
) {}
