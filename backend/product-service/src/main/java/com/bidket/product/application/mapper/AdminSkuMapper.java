package com.bidket.product.application.mapper;

import com.bidket.product.infrastructure.persistence.entity.ProductSku;
import com.bidket.product.presentation.dto.response.product.SkuGetAdminResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminSkuMapper {

    @Mapping(source = "id", target = "skuId")
    @Mapping(source = "size.id", target = "sizeId")
    @Mapping(source = "size.displayLabel", target = "sizeLabel")
    SkuGetAdminResponse toDto(ProductSku sku);
}
