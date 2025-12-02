package com.bidket.queue.domain.jwt;

import java.util.UUID;

public interface TokenProvider {
    String generateToken(UUID userId, UUID auctionId);
}
