package com.bidket.product.presentation.dto.response.category;

import java.util.UUID;

public record CategoryGetResponse(
        UUID id,
        Integer depth,
        String name
) {}
