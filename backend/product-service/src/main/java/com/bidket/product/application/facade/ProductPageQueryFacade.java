package com.bidket.product.application.facade;

import com.bidket.product.application.mapper.ProductPageMapper;
import com.bidket.product.application.service.ProductQueryService;
import com.bidket.product.application.service.ShoesDetailService;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.ProductShoesDetail;
import com.bidket.product.presentation.dto.response.category.CategoryGetResponse;
import com.bidket.product.presentation.dto.response.product.ProductPageGetResponse;
import com.bidket.product.presentation.dto.response.product.ProductPageGetResponse.ShoesDetailInfo;
import com.bidket.product.presentation.dto.response.product.SkuGetResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPageQueryFacade {

    private final ProductQueryService productQueryService;
    private final ShoesDetailService shoesDetailService;
    private final ProductPageMapper productPageMapper;

    public ProductPageGetResponse getPage(UUID productId) {

        // 1. Product 엔티티 조회
        Product product = productQueryService.getActiveProduct(productId);

        // 2. ShoesDetail 조회 -> DTO 변환 -> ProductPage DTO 변환
        ProductShoesDetail shoesDetailDto = shoesDetailService.getShoesDetail(product);

        ShoesDetailInfo shoesDetailInfo = productPageMapper.toShoesDetailInfo(shoesDetailDto);

        // 3. Category 조회 -> DTO 변환 -> CategoryInfo 변환
        List<CategoryGetResponse> categoryDtos =
                productQueryService.getCategoriesByProduct(product);

        List<ProductPageGetResponse.CategoryInfo> categoryInfos =
                categoryDtos.stream()
                        .map(category -> new ProductPageGetResponse.CategoryInfo(
                                category.id(),
                                category.name(),
                                category.depth()
                        ))
                        .toList();

        // 4. SKU 조회 -> SkuGetResponse 반환
        List<SkuGetResponse> skuDtos =
                productQueryService.getSkusByProductId(productId);

        // 5. 조합해서 ProductPage DTO 생성
        return productPageMapper.toPageResponse(
                product,
                shoesDetailInfo,
                categoryInfos,
                skuDtos
        );
    }
}
