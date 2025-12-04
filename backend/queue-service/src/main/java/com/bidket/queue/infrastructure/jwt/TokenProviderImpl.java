package com.bidket.queue.infrastructure.jwt;

import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.jwt.TokenProvider;
import com.bidket.queue.domain.model.QueueErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class TokenProviderImpl implements TokenProvider {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration}")
    private long expiration;

    @Override
    public String generateToken(UUID userId, UUID auctionId) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secret));
        return Jwts.builder()
                .claim("userId", userId)
                .claim("acutionId", auctionId)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusSeconds(expiration)))
                .signWith(key)
                .compact();
    }

    @Override
    public String extractToken(ServerRequest request) {
        String headerVal = request.headers().firstHeader("ACTIVE-TOKEN");
        assert headerVal != null;
        if(!headerVal.startsWith("Bearer") || headerVal.isBlank())
            throw new QueueException(QueueErrorCode.INVALID_TOKEN);

        return headerVal;
    }

    @Override
    public boolean validateToken(String token, UUID userId, UUID auctionId) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secret));
        UUID userIdPayload = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("userId", UUID.class);

        UUID auctionIdPayload = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("auctionId", UUID.class);

        return userId.equals(userIdPayload) && auctionId.equals(auctionIdPayload);
    }
}
