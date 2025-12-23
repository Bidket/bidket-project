package com.bidket.auction.application.service;

import com.bidket.auction.infrastructure.client.product.ProductClient;
import com.bidket.auction.infrastructure.client.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductClient productClient;

    public ProductResponse getProduct(UUID productId) {
        try {
            log.debug("Product 정보 조회: productId={}", productId);
            ProductResponse response = productClient.getProduct(productId);
            log.debug("Product 정보 조회 성공: productId={}, name={}", productId, response.name());
            return response;
        } catch (Exception e) {
            log.error("Product 정보 조회 실패: productId={}", productId, e);
            throw new RuntimeException("Product 정보 조회 실패: " + productId, e);
        }
    }

    public boolean isProductSizeAvailable(UUID productId, UUID sizeId) {
        try {
            ProductResponse product = getProduct(productId);
            return product.sizes().stream()
                .anyMatch(size -> size.sizeId().equals(sizeId) && size.stockQuantity() > 0);
        } catch (Exception e) {
            log.error("Product size 재고 확인 실패: productId={}, sizeId={}", productId, sizeId, e);
            return false;
        }
    }
}
