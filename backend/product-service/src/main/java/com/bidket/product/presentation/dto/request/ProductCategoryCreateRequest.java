package com.bidket.product.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProductCategoryCreateRequest(

        @NotNull(message = "카테고리 ID는 필수입니다.")
        UUID categoryId,

        Boolean isPrimary // true면 기존 primary 해제
) {}
