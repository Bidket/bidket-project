package com.bidket.product.application.mapper;

import com.bidket.product.infrastructure.persistence.entity.Category;
import com.bidket.product.presentation.dto.response.category.CategoryGetResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    // 단독 조회 DTO
    //CategoryGetResponse toGetResponse(Category category);
    List<CategoryGetResponse> toGetResponseList(List<Category> categories);

    // 상품 페이지 뷰 전용 DTO (ProductPage)
    //ProductPageGetResponse.CategoryInfo toCategoryInfo(Category category);
    //List<ProductPageGetResponse.CategoryInfo> toCategoryInfoList(List<Category> categories);
}
