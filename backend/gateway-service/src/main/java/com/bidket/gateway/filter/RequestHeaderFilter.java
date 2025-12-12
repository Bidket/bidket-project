package com.bidket.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Gateway 요청 헤더 필터
 * JWT 토큰에서 user_id와 role을 추출하여 요청 헤더에 추가합니다.
 * 하위 서비스는 이 헤더를 통해 사용자 정보를 확인할 수 있습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestHeaderFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    // X- 접두사 사용 이유:
    // 1. 커스텀 헤더임을 명확히 표시 (표준 HTTP 헤더와 구분)
    // 2. API Gateway에서 하위 서비스로 전달하는 메타데이터임을 명시
    // 3. 관례적으로 많이 사용됨 (RFC 6648에서는 권장하지 않지만 여전히 널리 사용)
    // 4. 프록시/로드밸런서에서 추가하는 헤더와 구분하기 쉬움
    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_USER_ROLE_HEADER = "X-User-Role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 요청에서 JWT 토큰 추출
        String token = extractToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                // JWT 토큰에서 user_id 추출
                UUID userId = jwtTokenProvider.getUserIdFromToken(token);
                
                if (userId == null) {
                    log.warn("Failed to extract userId from token");
                    return chain.filter(exchange);
                }
                
                // JWT 토큰에서 role 추출
                String role = jwtTokenProvider.getRoleFromToken(token);
                if (role == null || role.isEmpty()) {
                    role = "ROLE_USER"; // 기본값 (JWT에 role이 없는 경우)
                    log.debug("Role not found in token, using default: {}", role);
                }

                // 요청 헤더에 user_id와 role 추가
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header(X_USER_ID_HEADER, userId.toString())
                        .header(X_USER_ROLE_HEADER, role)
                        .build();

                log.info("Request header added - X-User-Id: {}, X-User-Role: {}", userId, role);

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (Exception e) {
                log.warn("Failed to extract user info from token: {}", e.getMessage(), e);
            }
        }

        // 토큰이 없거나 유효하지 않은 경우 원본 요청 그대로 전달
        return chain.filter(exchange);
    }

    /**
     * 요청 헤더에서 JWT 토큰 추출
     */
    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    @Override
    public int getOrder() {
        return -2; // ResponseHeaderFilter보다 먼저 실행
    }
}

