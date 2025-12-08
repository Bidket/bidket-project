package com.bidket.queue.domain.repository;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface QueueTrafficRepository {

    Mono<Long> addAllActiveUser(String activeKey, List<UUID> userIds);

    Mono<Long> getActiveUserCount(UUID auctionId);

    Mono<Long> kickActiveUser(UUID auctionId, UUID userId);

    Mono<Boolean> addWaitingUser(UUID auctionId, UUID userId);

    Mono<Long> getWaitingUserCount(UUID auctionId);

    Mono<Long> removeWaitingUser(String waitingKey, UUID userId);

    Mono<List<UUID>> popUserIdWaitingQueue(String waitingKey, long limit);

    Mono<Long> getRank(UUID auctionId, UUID userId);

    Mono<Boolean> saveToken(UUID auctionId, Map<UUID, String> tokens);

    Mono<String> getToken(UUID userId, UUID auctionId);
}
