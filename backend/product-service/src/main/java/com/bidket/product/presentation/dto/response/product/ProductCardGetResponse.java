package com.bidket.product.presentation.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "퍼블릭 상품 목록 조회 응답")
public record ProductCardGetResponse(
        UUID productId,

        String name,
        String nameKr,
        String modelCode,

        String brandName,
        String productTypeName,

        BigDecimal releasePrice,

        LocalDate releaseDate
) {}
