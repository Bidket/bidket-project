package com.bidket.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Component
public class QueueActiveTokenFilter extends AbstractGatewayFilterFactory {
    private final String Q_ACTIVE_TOKEN_HEADER = "Q-ACTIVE-TOKEN";
    private final QueueTokenProvider tokenProvider;

    public QueueActiveTokenFilter(QueueTokenProvider tokenProvider) {
        super(Config.class);
        this.tokenProvider = tokenProvider;
    }

    public static class Config {
    }

    @Override
    public GatewayFilter apply(Object config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = (ServerHttpRequest) exchange.getRequest();

            if (!request.getHeaders().containsKey(Q_ACTIVE_TOKEN_HEADER))
                return onError(exchange, "Missing Token", HttpStatus.UNAUTHORIZED);

            String token = Objects.requireNonNull(request.getHeaders().get(Q_ACTIVE_TOKEN_HEADER)).get(0);
            return tokenProvider.validateToken(token)
                    .flatMap(isValid -> {
                        if (isValid)
                            return chain.filter(exchange);
                        else
                            return onError(exchange, "Invalid Token", HttpStatus.UNAUTHORIZED);
                    });
        });
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);

        log.error("Queue Token Error: {} - Path: {}", err, exchange.getRequest().getPath());

        return response.setComplete();
    }
}
