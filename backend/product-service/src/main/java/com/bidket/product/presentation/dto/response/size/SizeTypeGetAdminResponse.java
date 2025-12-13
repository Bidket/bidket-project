package com.bidket.product.presentation.dto.response.size;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "사이즈 타입 목록 요청")
public record SizeTypeGetAdminResponse(

        @Schema(description = "사이즈 타입 ID", example = "UUID")
        UUID id,

        @Schema(description = "상품 타입 ID", example = "UUID")
        UUID productTypeId,

        @Schema(description = "사이즈 타입 코드", example = "SHOES_KR_MM")
        String code,

        @Schema(description = "사이즈 타입 지역 코드", example = "KR")
        String regionCode,

        @Schema(description = "사이즈 타입 설명", example = "한국 신발 사이즈 체계")
        String description,

        @Schema(description = "속한 상품 타입의 기본 사이즈 타입 여부", example = "true")
        Boolean isDefault,

        @Schema(description = "생성일", example = "2025-12-01T08:30:00")
        LocalDateTime createdAt,

        @Schema(description = "수정일", example = "2025-12-15T08:30:00")
        LocalDateTime updatedAt,

        @Schema(description = "생성자")
        String createdBy,

        @Schema(description = "수정자")
        String updatedBy,

        @Schema(description = "삭제일", example = "2025-12-30T08:30:00")
        LocalDateTime deletedAt
) {}
