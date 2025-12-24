package com.bidket.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class QueueTokenProvider {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final SecretKey secretKey;

    public QueueTokenProvider(@Value("${jwt.queue.secret}") String secret,
                              ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Mono<Boolean> validateToken(String token) {
        return Mono.fromCallable(() ->
                        Jwts.parser()
                                .verifyWith(secretKey)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload()
                )
                .onErrorResume(e -> Mono.empty())
                .flatMap(claims -> {
                    String auctionId = claims.get("auctionId").toString();
                    String userId = claims.get("userId").toString();
                    String key = "queue:token:" + auctionId;

                    return redisTemplate.opsForHash()
                            .get(key, userId)
                            .map(token::equals);
                })
                .switchIfEmpty(Mono.just(false));
    }

    public Mono<UUID> getUserId(String token) {
        return Mono.just(UUID.fromString(
                Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .get("userId")
                        .toString()
        ));
    }

    public Mono<UUID> getAuctionId(String token) {
        return Mono.just(UUID.fromString(
                Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .get("auctionId")
                        .toString()
        ));
    }

    public String extractToken(ServerRequest request) {
        String headerVal = request.headers().firstHeader("ACTIVE-TOKEN");
        assert headerVal != null;
        if (!headerVal.startsWith("Bearer") || headerVal.isBlank())
            return null;

        return headerVal;
    }
}
