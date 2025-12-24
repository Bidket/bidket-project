package com.bidket.user.infrastructure.external;

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
import java.util.List;
import java.util.UUID;

/**
 * Auction Service 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionServiceClient {

    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;

    @Value("${spring.cloud.discovery.enabled:true}")
    private boolean discoveryEnabled;

    @Value("${services.auction-service.name:auction-service}")
    private String auctionServiceName;

    @Value("${services.auction-service.url:http://localhost:8300}")
    private String auctionServiceUrl;

    /**
     * 내 입찰 내역 조회
     * @param userId 사용자 ID
     * @param authorizationToken Authorization 헤더 값 (Bearer 토큰)
     * @return 입찰 내역 응답
     */
    public BidListResponse getMyBids(UUID userId, String authorizationToken) {
        try {
            String baseUrl = getBaseUrl();
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/api/v1/bids/my")
                    .toUriString();

            log.debug("Auction Service 호출: GET {}", url);

            ParameterizedTypeReference<ApiResponseWrapper<BidListResponse>> responseType = 
                    new ParameterizedTypeReference<ApiResponseWrapper<BidListResponse>>() {};

            ApiResponseWrapper<BidListResponse> response = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorizationToken)
                    .header("X-User-Id", userId.toString())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.warn("Auction Service 호출 실패: userId={}, status={}", userId, res.getStatusCode());
                        throw new UserException(UserErrorCode.BAD_REQUEST);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("Auction Service 호출 실패: userId={}, status={}", userId, res.getStatusCode());
                        throw new UserException(UserErrorCode.AUCTION_SERVICE_UNAVAILABLE);
                    })
                    .body(responseType);

            if (response == null || response.getData() == null) {
                log.warn("Auction Service 응답 데이터 없음: userId={}", userId);
                throw new UserException(UserErrorCode.AUCTION_SERVICE_UNAVAILABLE);
            }
            
            return response.getData();

        } catch (UserException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Auction Service 호출 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new UserException(UserErrorCode.AUCTION_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Auction Service 호출 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new UserException(UserErrorCode.AUCTION_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 서비스 기본 URL 조회
     */
    private String getBaseUrl() {
        if (discoveryEnabled) {
            try {
                ServiceInstance instance = discoveryClient.getInstances(auctionServiceName)
                        .stream()
                        .findFirst()
                        .orElse(null);

                if (instance != null) {
                    return instance.getUri().toString();
                }
            } catch (Exception e) {
                log.warn("Eureka에서 Auction Service 인스턴스를 찾을 수 없습니다: {}", e.getMessage());
            }
        }
        return auctionServiceUrl;
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
     * 입찰 내역 응답 DTO
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BidListResponse {
        private List<BidResponse> bids;
        private Long totalCount;
        private Integer currentPage;
        private Integer totalPages;
    }

    /**
     * 입찰 응답 DTO
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BidResponse {
        private UUID id;
        private UUID auctionId;
        private UUID bidderId;
        private Long amount;
        private Boolean highest;
        private String status;
        private Integer rank;
        private UUID orderId;
        private LocalDateTime createdAt;
    }
}

