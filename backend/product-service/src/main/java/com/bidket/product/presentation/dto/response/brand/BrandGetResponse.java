package com.bidket.product.presentation.dto.response.brand;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "브랜드 목록 조회 응답")
public record BrandGetResponse(
        UUID brandId,
        String name
) {}
