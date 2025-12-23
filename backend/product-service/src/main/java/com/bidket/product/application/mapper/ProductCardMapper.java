package com.bidket.product.application.mapper;

import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.presentation.dto.response.product.ProductCardGetResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductCardMapper {
    @Mapping(source = "id", target = "productId")
    @Mapping(source = "brand.name", target = "brandName")
    @Mapping(source = "productType.name", target = "productTypeName")
    ProductCardGetResponse toDto(Product product);
}
