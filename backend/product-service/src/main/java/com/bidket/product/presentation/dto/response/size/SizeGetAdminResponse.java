package com.bidket.product.presentation.dto.response.size;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "사이즈 목록 요청")
public record SizeGetAdminResponse(

        @Schema(description = "사이즈 ID", example = "UUID")
        UUID id,

        @Schema(description = "사이즈 타입 ID", example = "UUID")
        UUID sizeTypeId,

        @Schema(description = "사이즈 코드", example = "260")
        String code,

        @Schema(description = "사이즈 화면 표시용 값", example = "260mm")
        String displayLabel,

        @Schema(description = "사이즈 정렬 인덱스", example = "100")
        Long sortId,

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
