package com.bidket.product.application.mapper;

import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductCategory;
import com.bidket.product.infrastructure.persistence.entity.ProductShoesDetail;
import com.bidket.product.infrastructure.persistence.entity.ProductSku;
import com.bidket.product.presentation.dto.response.product.ProductGetAdminDetailResponse;
import com.bidket.product.presentation.dto.response.product.ProductGetAdminDetailResponse.CategoryInfo;
import com.bidket.product.presentation.dto.response.product.ProductGetAdminDetailResponse.ShoesDetailInfo;
import com.bidket.product.presentation.dto.response.product.SkuGetAdminResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdminProductDetailMapper {

    default ProductGetAdminDetailResponse toDetailResponse(
            Product product,
            List<ProductCategory> productCategories,
            List<ProductSku> skus,
            ProductShoesDetail shoesDetail
    ) {
        return new ProductGetAdminDetailResponse(
                product.getId(),
                product.getName(),
                product.getNameKr(),
                product.getModelCode(),
                product.getGender(),
                product.getDescription(),
                product.getReleaseDate(),
                product.getReleasePrice(),
                product.getStatus(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.getBrand().getNameKr(),
                product.getProductType().getId(),
                product.getProductType().getName(),
                mapCategories(productCategories),
                mapSkus(skus),
                mapShoesDetail(shoesDetail),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getDeletedAt()
        );
    }

    /** 카테고리 매핑 */
    private static List<ProductGetAdminDetailResponse.CategoryInfo> mapCategories(
            List<ProductCategory> productCategories
    ) {
        return productCategories.stream()
                .map(category -> new CategoryInfo(
                        category.getCategory().getId(),
                        category.getCategory().getName(),
                        category.getCategory().getDepth(),
                        category.getIsPrimary()
                ))
                .toList();
    }

    /** SKU 매핑 */
    private static List<SkuGetAdminResponse> mapSkus(
            List<ProductSku> skus
    ) {
        return skus.stream()
                .map(sku -> new SkuGetAdminResponse(
                        sku.getId(),
                        sku.getSkuCode(),
                        sku.getSize().getId(),
                        sku.getSize().getDisplayLabel(),
                        sku.getStatus(),
                        sku.getCreatedAt(),
                        sku.getUpdatedAt()
                ))
                .toList();
    }

    /** 신발 상세 매핑 */
    private static ProductGetAdminDetailResponse.ShoesDetailInfo mapShoesDetail(
            ProductShoesDetail shoesDetail
    ) {
        if (shoesDetail == null) return null;

        return new ShoesDetailInfo(
                shoesDetail.getColorway(),
                shoesDetail.getMainMaterial(),
                shoesDetail.getSilhouette(),
                shoesDetail.getStyle(),
                shoesDetail.getOriginCountry(),
                shoesDetail.getWeight()
        );
    }
}
