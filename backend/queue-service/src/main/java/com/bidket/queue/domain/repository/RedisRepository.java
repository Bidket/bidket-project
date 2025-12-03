package com.bidket.queue.domain.repository;

import com.bidket.queue.domain.model.QueueConfigModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RedisRepository {
    Mono<Boolean> saveConfig(String configKey, QueueConfigModel model);

    Mono<QueueConfigModel> getConfig(String configKey);

    Mono<Boolean> setExpiration(String configKey, Instant expireAt);

    Mono<Boolean> deleteConfig(String configKey);

    Mono<Long> registerActiveAuction(UUID auctionId);

    Flux<UUID> getAllActiveAuctions();

    Mono<Long> removeActiveAuction(UUID auctionId);

    Mono<Boolean> addActiveUser(String activeKey, Long maxUser, UUID userId);

    Mono<Long> addAllActiveUser(String activeKey, List<UUID> userIds);

    Mono<Long> activeUserCount(String activeKey);

    Mono<Boolean> addWaitingUser(String waitingKey, UUID userId);

    Mono<List<UUID>> popUserIdWaitingQueue(String waitingKey, long limit);

    Mono<Long> getRank(String waitingKey, UUID userId);
}
