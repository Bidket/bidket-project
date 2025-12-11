package com.bidket.product.presentation.dto.response.category;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "카테고리 조회 응답")
public record CategoryGetResponse(

        @Schema(description = "카테고리 ID", example = "UUID")
        UUID id,

        @Schema(description = "자동 계산된 카테고리 깊이", example = "1")
        Integer depth,

        @Schema(description = "카테고리명", example = "운동화")
        String name
) {}
