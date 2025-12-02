package com.bidket.queue.infrastructure.jwt;

import com.bidket.queue.domain.jwt.TokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

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
}
