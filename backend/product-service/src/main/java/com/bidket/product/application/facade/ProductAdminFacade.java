package com.bidket.product.application.facade;

import com.bidket.product.application.service.ProductAdminService;
import com.bidket.product.application.service.ShoesDetailService;
import com.bidket.product.presentation.dto.request.product.ProductWithShoesCreateRequest;
import com.bidket.product.presentation.dto.request.shoesdetail.ProductShoesDetailCreateRequest;
import com.bidket.product.presentation.dto.response.product.ProductCreateResponse;
import com.bidket.product.presentation.dto.response.product.ProductWithShoesCreateResponse;
import com.bidket.product.presentation.dto.response.shoesdetail.ProductShoesDetailCreateResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductAdminFacade {

    private final ProductAdminService productAdminService;
    private final ShoesDetailService shoesDetailService;

    @Transactional
    public ProductWithShoesCreateResponse createProductWithShoes(
            ProductWithShoesCreateRequest bundleReq
    ) {

        ProductCreateResponse productRes =
                productAdminService.createProduct(bundleReq.product());
        UUID productId = productRes.id();

        ProductShoesDetailCreateRequest origin = bundleReq.shoesDetail();
        ProductShoesDetailCreateRequest shoesReq = new ProductShoesDetailCreateRequest(
                origin.colorway(),
                origin.mainMaterial(),
                origin.silhouette(),
                origin.style(),
                origin.originCountry(),
                origin.weight()
        );

        ProductShoesDetailCreateResponse shoesRes =
                shoesDetailService.createShoesDetail(productId, shoesReq);

        return new ProductWithShoesCreateResponse(productRes, shoesRes);
    }
}
