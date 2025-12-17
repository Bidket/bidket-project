package com.bidket.auction.infrastructure.client.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
    @JsonProperty("productId") UUID productId,
    @JsonProperty("name") String name,
    @JsonProperty("brand") String brand,
    @JsonProperty("category") String category,
    @JsonProperty("retailPrice") BigDecimal retailPrice,
    @JsonProperty("description") String description,
    @JsonProperty("sizes") List<ProductSizeResponse> sizes
) {
    public record ProductSizeResponse(
        @JsonProperty("sizeId") UUID sizeId,
        @JsonProperty("size") String size,
        @JsonProperty("stockQuantity") Integer stockQuantity
    ) {
    }
}
