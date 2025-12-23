package com.bidket.product.presentation.dto.response.category;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "카테고리 트리 조회 응답")
public record CategoryTreeResponse(

        UUID categoryId,
        String name,
        int depth,
        List<CategoryTreeResponse> children
) {}
