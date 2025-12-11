package com.bidket.product.application.mapper;

import com.bidket.product.infrastructure.persistence.entity.ProductSku;
import com.bidket.product.presentation.dto.response.product.SkuGetResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SkuMapper {

    @Mapping(target = "productId", source = "entity.product.id")
    @Mapping(target = "sizeId", source = "entity.size.id")
    @Mapping(target = "skuCode", source = "entity.skuCode")
    @Mapping(target = "status", source = "entity.status")
    SkuGetResponse toGetResponse(ProductSku entity);

    List<SkuGetResponse> toGetResponseList(List<ProductSku> entities);
}
