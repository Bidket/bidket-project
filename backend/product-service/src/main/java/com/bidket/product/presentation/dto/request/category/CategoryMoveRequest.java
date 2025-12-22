package com.bidket.product.presentation.dto.request.category;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "카테고리 이동 요청")
public record CategoryMoveRequest(
        @Schema(
                description = "이동할 부모 카테고리 ID (루트로 이동 시 null)",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
        )
        UUID newParentId
) {}
