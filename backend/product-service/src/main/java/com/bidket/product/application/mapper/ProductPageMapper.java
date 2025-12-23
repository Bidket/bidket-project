package com.bidket.product.application.mapper;

import com.bidket.product.domain.model.ProductDetail;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductCategory;
import com.bidket.product.infrastructure.persistence.entity.ProductShoesDetail;
import com.bidket.product.presentation.dto.response.product.ProductPageGetResponse;
import com.bidket.product.presentation.dto.response.product.SkuGetResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {SkuMapper.class})
public interface ProductPageMapper {

    /** ShoesDetail */
    // ShoesDetail 엔티티 -> ProductPage 내부 DTO로 변환
    ProductPageGetResponse.ShoesDetailInfo toShoesDetailInfo(ProductShoesDetail shoesDetail);

    default ProductPageGetResponse.ShoesDetailInfo mapShoesDetail(
            ProductDetail detail
    ) {
        if (detail instanceof ProductShoesDetail shoesDetail) {
            return toShoesDetailInfo(shoesDetail);
        }
        return null;
    }

    /** Category */
    default List<ProductPageGetResponse.CategoryInfo> mapCategories(
            List<ProductCategory> categories
    ) {
        return categories.stream()
                .map(pc -> new ProductPageGetResponse.CategoryInfo(
                        pc.getCategory().getId(),
                        pc.getCategory().getName(),
                        pc.getCategory().getDepth()
                ))
                .toList();
    }

    /** ProductPage로 조립 */
    @Mapping(target = "brandName", source = "product.brand.name")
    @Mapping(target = "productTypeName", source = "product.productType.name")
    ProductPageGetResponse toPageResponse(
            Product product,
            ProductPageGetResponse.ShoesDetailInfo shoesDetail,
            List<ProductPageGetResponse.CategoryInfo> categories,
            List<SkuGetResponse> skus
    );
}
