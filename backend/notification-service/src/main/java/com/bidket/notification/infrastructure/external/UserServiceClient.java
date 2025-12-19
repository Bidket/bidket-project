package com.bidket.notification.infrastructure.external;

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
 * User Service 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;

    @Value("${spring.cloud.discovery.enabled:true}")
    private boolean discoveryEnabled;

    @Value("${services.user-service.name:user-service}")
    private String userServiceName;

    @Value("${services.user-service.url:http://localhost:8100}")
    private String userServiceUrl;

    /**
     * 사용자 이메일 조회
     * @param userId 사용자 ID
     * @return 이메일 주소 (실패 시 null)
     */
    public String getUserEmail(UUID userId) {
        try {
            String baseUrl = getBaseUrl();
            String url = baseUrl + "/api/v1/users/" + userId + "/email";

            log.debug("User Service 호출: GET {}", url);

            String email = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            log.debug("User Service 응답: userId={}, email={}", userId, email);
            return email;

        } catch (RestClientException e) {
            log.warn("User Service 이메일 조회 실패: userId={}, error={}", userId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("User Service 이메일 조회 중 예상치 못한 오류: userId={}, error={}", userId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 서비스 기본 URL 조회
     */
    private String getBaseUrl() {
        if (discoveryEnabled) {
            try {
                ServiceInstance instance = discoveryClient.getInstances(userServiceName)
                        .stream()
                        .findFirst()
                        .orElse(null);

                if (instance != null) {
                    return instance.getUri().toString();
                }
            } catch (Exception e) {
                log.warn("Eureka에서 User Service 인스턴스를 찾을 수 없습니다: {}", e.getMessage());
            }
        }
        return userServiceUrl;
    }
}

