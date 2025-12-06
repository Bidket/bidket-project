package com.bidket.product.presentation.dto.request;

import com.bidket.product.domain.model.Silhouette;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductShoesDetailCreateRequest(

        @NotNull(message = "상품 ID는 필수입니다.")
        UUID productId,

        @NotBlank(message = "컬러웨이는 필수입니다.")
        @Size(max = 50, message = "컬러웨이는 최대 50자입니다.")
        String colorway,

        @NotBlank(message = "메인 소재는 필수입니다.")
        @Size(max = 50, message = "메인 소재는 최대 50자입니다.")
        String mainMaterial,

        @NotBlank(message = "실루엣은 필수입니다.")
        Silhouette silhouette,

        @Size(max = 100, message = "스타일은 최대 100자입니다.")
        String style,

        @Pattern(regexp = "^[A-Z]+$", message = "영어 대문자만 입력가능합니다.")
        @Size(max = 10, message = "생산국은 최대 10자입니다.")
        String originCountry,

        @DecimalMin(value = "0.0", inclusive = true, message = "무게는 0 이상이어야 합니다.")
        BigDecimal weight
) {}
