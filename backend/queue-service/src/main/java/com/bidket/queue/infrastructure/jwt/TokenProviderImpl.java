package com.bidket.queue.infrastructure.jwt;

import com.bidket.queue.domain.jwt.TokenProvider;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class TokenProviderImpl implements TokenProvider {
    private Key key;

    @Override
    public String generateToken(UUID userId, UUID auctionId) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("acutionId", auctionId)
                .setIssuedAt(Date.from(Instant.now()))
                .signWith(key)
                .compact();
    }
}
