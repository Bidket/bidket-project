package com.bidket.user.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient 설정
 * 소셜 로그인 API 호출을 위한 타임아웃 설정 포함
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        ClientHttpRequestFactory requestFactory = createRequestFactory();
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * HTTP 요청 팩토리 생성 (타임아웃 설정 포함)
     * Connection timeout: 5초 (연결 시도 최대 시간)
     * Read timeout: 10초 (데이터 읽기 최대 시간)
     */
    private ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return factory;
    }
}

