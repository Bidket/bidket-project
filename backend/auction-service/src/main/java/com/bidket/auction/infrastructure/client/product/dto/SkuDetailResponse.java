package com.bidket.auction.infrastructure.client.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record SkuDetailResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("skuCode") String skuCode,
    @JsonProperty("skuStatus") String skuStatus,
    @JsonProperty("sizeId") UUID sizeId,
    @JsonProperty("sizeLabel") String sizeLabel,
    @JsonProperty("productId") UUID productId,
    @JsonProperty("productName") String productName,
    @JsonProperty("brandName") String brandName,
    @JsonProperty("modelCode") String modelCode,
    @JsonProperty("gender") String gender,
    @JsonProperty("releasePrice") BigDecimal releasePrice
) {
}
