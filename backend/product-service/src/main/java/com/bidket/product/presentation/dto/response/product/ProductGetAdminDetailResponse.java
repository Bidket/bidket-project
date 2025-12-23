package com.bidket.product.presentation.dto.response.product;

import com.bidket.product.domain.model.Gender;
import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.domain.model.Silhouette;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "어드민 상품 상세 조회 응답")
public record ProductGetAdminDetailResponse(

        UUID productId,

        String name,
        String nameKr,
        String modelCode,
        Gender gender,

        String description,
        LocalDate releaseDate,
        BigDecimal releasePrice,

        ProductStatus status,

        UUID brandId,
        String brandName,
        String brandNameKr,

        UUID productTypeId,
        String productTypeName,

        List<CategoryInfo> categories,

        List<SkuGetAdminResponse> skus,

        ShoesDetailInfo shoesDetail,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {

    public record CategoryInfo(
            UUID id,
            String name,
            int depth,
            Boolean isPrimary
    ) {}

    public record ShoesDetailInfo(
            String colorway,
            String mainMaterial,
            Silhouette silhouette,
            String style,
            String originCountry,
            BigDecimal weight
    ) {}
}
