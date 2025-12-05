package com.bidket.queue.domain.repository;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface QueueTrafficRepository {

    Mono<Long> addAllActiveUser(String activeKey, List<UUID> userIds);

    Mono<Long> getActiveUserCount(String activeKey);

    Mono<Boolean> addWaitingUser(String waitingKey, UUID userId);

    Mono<Long> removeWaitingUser(String waitingKey, UUID userId);

    Mono<List<UUID>> popUserIdWaitingQueue(String waitingKey, long limit);

    Mono<Long> getRank(String waitingKey, UUID userId);

    Mono<Boolean> saveToken(String tokenKey, Map<UUID, String> tokens);

    Mono<String> getToken(String tokenKey, UUID userId);
}
