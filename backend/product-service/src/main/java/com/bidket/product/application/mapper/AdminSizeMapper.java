package com.bidket.product.application.mapper;

import com.bidket.product.infrastructure.persistence.entity.Size;
import com.bidket.product.infrastructure.persistence.entity.SizeType;
import com.bidket.product.presentation.dto.response.size.SizeGetAdminResponse;
import com.bidket.product.presentation.dto.response.size.SizeTypeGetAdminResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminSizeMapper {

    // Size Type
    @Mapping(source = "productType.id", target = "productTypeId")
    SizeTypeGetAdminResponse toSizeTypeDto(SizeType sizeType);

    // Size
    @Mapping(source = "sizeType.id", target = "sizeTypeId")
    SizeGetAdminResponse toSizeDto(Size size);

    List<SizeGetAdminResponse> toSizeDtos(List<Size> sizes);

}