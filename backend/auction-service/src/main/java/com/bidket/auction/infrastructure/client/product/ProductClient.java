package com.bidket.auction.infrastructure.client.product;

import com.bidket.auction.infrastructure.client.product.dto.ProductResponse;
import com.bidket.auction.infrastructure.client.product.dto.SkuDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
    name = "product-service",
    url = "${feign.product-service.url}"
)
public interface ProductClient {

    @GetMapping("/v1/products/{productId}")
    ProductResponse getProduct(@PathVariable("productId") UUID productId);

    @GetMapping("/v1/products/skus/{skuId}")
    SkuDetailResponse getSkuDetail(@PathVariable("skuId") UUID skuId);
}
