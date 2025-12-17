package com.bidket.product.application.mapper;

import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductShoesDetail;
import com.bidket.product.presentation.dto.response.product.ProductPageGetResponse;
import com.bidket.product.presentation.dto.response.product.SkuGetResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductPageMapper {

    // ShoesDetail 엔티티 -> ProductPage 내부 DTO로 변환
    ProductPageGetResponse.ShoesDetailInfo toShoesDetailInfo(ProductShoesDetail shoesDetail);

    // ProductPage로 조립
    @Mapping(target = "brandName", source = "product.brand.name")
    @Mapping(target = "productTypeName", source = "product.productType.name")
    ProductPageGetResponse toPageResponse(
            Product product,
            ProductPageGetResponse.ShoesDetailInfo shoesDetail,
            List<ProductPageGetResponse.CategoryInfo> categories,
            List<SkuGetResponse> skus
    );
}
