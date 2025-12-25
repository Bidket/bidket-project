package com.bidket.user.infrastructure.external;

import com.bidket.common.presentation.response.PageResponse;
import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Order Service 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderServiceClient {

    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;

    @Value("${spring.cloud.discovery.enabled:true}")
    private boolean discoveryEnabled;

    @Value("${services.order-service.name:order-service}")
    private String orderServiceName;

    @Value("${services.order-service.url:http://localhost:8500}")
    private String orderServiceUrl;

    /**
     * 내 주문/결제 내역 조회
     * @param userId 사용자 ID
     * @param page 페이지 번호
     * @param size 페이지 사이즈
     * @param authorizationToken Authorization 헤더 값 (Bearer 토큰)
     * @return 주문 내역 응답
     */
    public PageResponse<OrderSummaryResponse> getOrders(UUID userId, int page, int size, String authorizationToken) {
        try {
            String baseUrl = getBaseUrl();
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/v1/orders")
                    .queryParam("userId", userId)
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .toUriString();

            log.debug("Order Service 호출: GET {}", url);

            ParameterizedTypeReference<ApiResponseWrapper<PageResponseWrapper<OrderSummaryResponse>>> responseType = 
                    new ParameterizedTypeReference<ApiResponseWrapper<PageResponseWrapper<OrderSummaryResponse>>>() {};

            ApiResponseWrapper<PageResponseWrapper<OrderSummaryResponse>> response = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorizationToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.warn("Order Service 호출 실패: userId={}, status={}", userId, res.getStatusCode());
                        throw new UserException(UserErrorCode.BAD_REQUEST);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("Order Service 호출 실패: userId={}, status={}", userId, res.getStatusCode());
                        throw new UserException(UserErrorCode.ORDER_SERVICE_UNAVAILABLE);
                    })
                    .body(responseType);

            if (response == null || response.getData() == null) {
                log.warn("Order Service 응답 데이터 없음: userId={}", userId);
                throw new UserException(UserErrorCode.ORDER_SERVICE_UNAVAILABLE);
            }
            
            PageResponseWrapper<OrderSummaryResponse> pageResponseWrapper = response.getData();
            
            return PageResponse.of(
                    pageResponseWrapper.getContent(),
                    pageResponseWrapper.getPage() != null ? pageResponseWrapper.getPage() : page,
                    pageResponseWrapper.getSize() != null ? pageResponseWrapper.getSize() : size,
                    pageResponseWrapper.getTotalElements() != null ? pageResponseWrapper.getTotalElements() : 0L
            );

        } catch (UserException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Order Service 호출 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new UserException(UserErrorCode.ORDER_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Order Service 호출 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new UserException(UserErrorCode.ORDER_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 서비스 기본 URL 조회
     */
    private String getBaseUrl() {
        if (discoveryEnabled) {
            try {
                ServiceInstance instance = discoveryClient.getInstances(orderServiceName)
                        .stream()
                        .findFirst()
                        .orElse(null);

                if (instance != null) {
                    return instance.getUri().toString();
                }
            } catch (Exception e) {
                log.warn("Eureka에서 Order Service 인스턴스를 찾을 수 없습니다: {}", e.getMessage());
            }
        }
        return orderServiceUrl;
    }

    /**
     * ApiResponse 래퍼 DTO (역직렬화 문제 해결용)
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiResponseWrapper<T> {
        @JsonProperty("data")
        private T data;
    }

    /**
     * PageResponse 래퍼 DTO (역직렬화 문제 해결용)
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageResponseWrapper<T> {
        @JsonProperty("content")
        private java.util.List<T> content;
        
        @JsonProperty("totalElements")
        private Long totalElements;
        
        @JsonProperty("page")
        private Integer page;
        
        @JsonProperty("size")
        private Integer size;
    }

    /**
     * 주문 요약 응답 DTO
     * Order 서비스의 전체 응답 구조를 그대로 반영 (다른 곳에서도 사용 가능)
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderSummaryResponse {
        @JsonProperty("orderId")
        private String orderId;
        
        @JsonProperty("userId")
        private UUID userId;
        
        @JsonProperty("auctionId")
        private String auctionId;
        
        @JsonProperty("shoeId")
        private String shoeId;
        
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("amount")
        private Long amount;
        
        @JsonProperty("usedPointAmount")
        private Long usedPointAmount;
        
        @JsonProperty("paymentExpiredAt")
        private LocalDateTime paymentExpiredAt;
        
        @JsonProperty("createdAt")
        private LocalDateTime createdAt;
        
        @JsonProperty("updatedAt")
        private LocalDateTime updatedAt;
        
        @JsonProperty("productName")
        private String productName;
        
        @JsonProperty("auctionTitle")
        private String auctionTitle;
        
        @JsonProperty("auctionStartTime")
        private LocalDateTime auctionStartTime;
    }
}

