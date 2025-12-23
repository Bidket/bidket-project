package com.bidket.product.application.mapper;

import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.presentation.dto.response.product.ProductGetAdminResponse;
import com.bidket.product.presentation.dto.response.product.ProductGetAdminSimpleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminProductMapper {

    /** 목록 조회 */
    @Mapping(source = "id", target = "productId")
    @Mapping(source = "productType.id", target = "productTypeId")
    @Mapping(source = "brand.id", target = "brandId")
    ProductGetAdminResponse toListDto(Product product);

    /** 단건 단순 조회 */
    @Mapping(source = "id", target = "productId")
    @Mapping(source = "productType.id", target = "productTypeId")
    @Mapping(source = "brand.id", target = "brandId")
    ProductGetAdminSimpleResponse toSimpleDto(Product product);
}
