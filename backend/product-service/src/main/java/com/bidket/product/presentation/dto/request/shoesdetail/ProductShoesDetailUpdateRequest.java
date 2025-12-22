package com.bidket.product.presentation.dto.request.shoesdetail;

import com.bidket.product.domain.model.Silhouette;
import com.bidket.product.presentation.dto.request.product.ProductDetailUpdateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "신발 상품 수정 요청")
public record ProductShoesDetailUpdateRequest(

        @Schema(description = "신발 컬러웨이", example = "Black/White")
        @Size(max = 50, message = "컬러웨이는 최대 50자입니다.")
        String colorway,

        @Schema(description = "신발 메인 소재", example = "Leather")
        @Size(max = 50, message = "메인 소재는 최대 50자입니다.")
        String mainMaterial,

        @Schema(description = "신발 실루엣", example = "LOW")
        Silhouette silhouette,

        @Schema(description = "신발 스타일", example = "Basketball")
        @Size(max = 100, message = "스타일은 최대 100자입니다.")
        String style,

        @Schema(description = "신발 생산지", example = "VN")
        @Pattern(regexp = "^[A-Z]+$", message = "영어 대문자만 입력가능합니다.")
        @Size(max = 10, message = "생산국은 최대 10자입니다.")
        String originCountry,

        @Schema(description = "신발 무게(g)", example = "800")
        @DecimalMin(value = "0.0", inclusive = true, message = "무게는 0 이상이어야 합니다.")
        BigDecimal weight
) implements ProductDetailUpdateRequest {}
