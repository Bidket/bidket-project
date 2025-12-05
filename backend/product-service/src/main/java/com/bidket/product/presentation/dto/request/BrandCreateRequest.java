package com.bidket.product.presentation.dto.request;

public record BrandCreateRequest(
        String name,
        String nameKr,
        String originCountry,
        String websiteUrl
) {}
