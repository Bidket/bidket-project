package com.bidket.notification.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
     * 경매 정보 조회
     * @param auctionId 경매 ID
     * @return 경매 정보 (실패 시 null)
     */
    public AuctionInfo getAuctionInfo(UUID auctionId) {
        try {
            String baseUrl = getBaseUrl();
            String url = baseUrl + "/api/v1/auctions/" + auctionId;

            log.debug("Auction Service 호출: GET {}", url);

            AuctionInfo auctionInfo = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(AuctionInfo.class);

            log.debug("Auction Service 응답: auctionId={}, name={}", auctionId, 
                    auctionInfo != null ? auctionInfo.getName() : null);
            return auctionInfo;

        } catch (RestClientException e) {
            log.warn("Auction Service 경매 정보 조회 실패: auctionId={}, error={}", auctionId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Auction Service 경매 정보 조회 중 예상치 못한 오류: auctionId={}, error={}", 
                    auctionId, e.getMessage(), e);
            return null;
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
     * 경매 정보 DTO
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuctionInfo {
        private UUID id;
        private String name;
        private String productName;
    }
}

