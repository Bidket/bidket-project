package com.bidket.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Gateway 응답 헤더 필터
 * 모든 응답에 회원 ID와 Role을 헤더로 추가합니다.
 * - 로그인 응답: 응답 본문에서 memberId와 role 추출
 * - 다른 요청: JWT 토큰에서 memberId 추출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseHeaderFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_USER_ROLE_HEADER = "X-User-Role";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 요청에서 JWT 토큰 추출
        String token = extractToken(request);
        final UUID userIdFromToken;
        final String roleFromToken;
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            userIdFromToken = jwtTokenProvider.getUserIdFromToken(token);
            roleFromToken = jwtTokenProvider.getRoleFromToken(token);
        } else {
            userIdFromToken = null;
            roleFromToken = null;
        }

        // 응답 본문을 읽기 위해 응답을 래핑
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(response) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux<?>) {
                    @SuppressWarnings("unchecked")
                    Flux<DataBuffer> fluxBody = (Flux<DataBuffer>) body;
                    
                    return DataBufferUtils.join(fluxBody)
                            .flatMap(dataBuffer -> {
                                byte[] content = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(content);
                                DataBufferUtils.release(dataBuffer);
                                
                                String bodyStr = new String(content, StandardCharsets.UTF_8);
                                
                                try {
                                    // 응답 본문에서 memberId와 role 추출 시도
                                    JsonNode jsonNode = objectMapper.readTree(bodyStr);
                                    
                                    UUID userId = null;
                                    String role = null;
                                    
                                    // 응답 본문에서 data 필드 확인 (ApiResponse 구조)
                                    JsonNode dataNode = jsonNode.get("data");
                                    if (dataNode != null) {
                                        JsonNode memberIdNode = dataNode.get("memberId");
                                        JsonNode roleNode = dataNode.get("role");
                                        
                                        if (memberIdNode != null && !memberIdNode.isNull()) {
                                            userId = UUID.fromString(memberIdNode.asText());
                                        }
                                        if (roleNode != null && !roleNode.isNull()) {
                                            role = roleNode.asText();
                                        }
                                    }
                                    
                                    // 응답 본문에서 찾지 못한 경우 JWT 토큰에서 추출
                                    if (userId == null && userIdFromToken != null) {
                                        userId = userIdFromToken;
                                    }
                                    if (role == null && roleFromToken != null) {
                                        role = roleFromToken;
                                    }
                                    // JWT에서도 role을 찾지 못한 경우 기본값 사용
                                    if (role == null || role.isEmpty()) {
                                        role = "ROLE_USER";
                                    }
                                    
                                    // 헤더 추가 (role은 이미 기본값이 설정되어 null이 될 수 없음)
                                    if (userId != null) {
                                        getHeaders().add(X_USER_ID_HEADER, userId.toString());
                                    }
                                    getHeaders().add(X_USER_ROLE_HEADER, role);
                                    
                                } catch (Exception e) {
                                    // JSON 파싱 실패 시 JWT 토큰에서만 추출
                                    if (userIdFromToken != null) {
                                        getHeaders().add(X_USER_ID_HEADER, userIdFromToken.toString());
                                    }
                                    if (roleFromToken != null && !roleFromToken.isEmpty()) {
                                        getHeaders().add(X_USER_ROLE_HEADER, roleFromToken);
                                    } else {
                                        getHeaders().add(X_USER_ROLE_HEADER, "ROLE_USER");
                                    }
                                }
                                
                                // 원본 응답 본문으로 다시 생성
                                DataBuffer buffer = response.bufferFactory().wrap(content);
                                return super.writeWith(Mono.just(buffer));
                            })
                            .onErrorResume(err -> {
                                // 에러 발생 시 원본 응답 그대로 전달
                                return super.writeWith(body);
                            });
                }
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
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
        return -1; // 다른 필터보다 먼저 실행
    }
}

