package com.bidket.product.application.mapper;

import com.bidket.product.infrastructure.persistence.entity.ProductSku;
import com.bidket.product.presentation.dto.response.product.SkuGetDetailResponse;
import com.bidket.product.presentation.dto.response.product.SkuGetResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SkuMapper {

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "skuCode", source = "entity.skuCode")
    @Mapping(target = "skuStatus", source = "entity.status")

    @Mapping(target = "sizeId", source = "entity.size.id")
    @Mapping(target = "sizeLabel", source = "entity.size.displayLabel")

    @Mapping(target = "productId", source = "entity.product.id")
    @Mapping(target = "productName", source = "entity.product.name")
    @Mapping(target = "brandName", source = "entity.product.brand.name")
    @Mapping(target = "modelCode", source = "entity.product.modelCode")
    @Mapping(target = "gender", source = "entity.product.gender")
    @Mapping(target = "releasePrice", source = "entity.product.releasePrice")
    SkuGetDetailResponse toDetailResponse(ProductSku entity);

    @Mapping(target = "productId", source = "entity.product.id")
    @Mapping(target = "sizeId", source = "entity.size.id")
    @Mapping(target = "skuCode", source = "entity.skuCode")
    @Mapping(target = "status", source = "entity.status")
    SkuGetResponse toSimpleResponse(ProductSku entity);

    List<SkuGetResponse> toGetResponseList(List<ProductSku> entities);
}
