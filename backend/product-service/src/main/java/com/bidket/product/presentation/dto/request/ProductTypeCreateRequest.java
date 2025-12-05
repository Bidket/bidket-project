package com.bidket.product.presentation.dto.request;

public record ProductTypeCreateRequest(
        String code,
        String name,
        String description
) {}
