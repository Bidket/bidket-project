package com.bidket.product.presentation.dto.request;

import java.util.UUID;

public record CategoryCreateRequest(
        UUID productTypeId,
        UUID parentId, //nullable
        String name,
        String code,
        Long sortId
) {}
