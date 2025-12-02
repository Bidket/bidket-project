package com.bidket.queue.domain.jwt;

import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.UUID;

public interface TokenProvider {
    String generateToken(UUID userId, UUID auctionId);
    String extractToken(ServerRequest request);
    boolean validateToken(String token, UUID userId, UUID auctionId);
}
