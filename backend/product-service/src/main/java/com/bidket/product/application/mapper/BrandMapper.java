package com.bidket.product.application.mapper;

import com.bidket.product.infrastructure.persistence.entity.Brand;
import com.bidket.product.presentation.dto.response.brand.BrandGetResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    @Mapping(source = "id", target = "brandId")
    BrandGetResponse toDto(Brand brand);
}
