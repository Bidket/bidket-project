package com.bidket.queue.infrastructure.redis;

import com.bidket.queue.domain.model.QueueConfigModel;
import com.bidket.queue.domain.repository.RedisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {
    private final ReactiveRedisOperations<String, Object> redisOps;
    private final ObjectMapper objectMapper;

    private static final String GLOBAL_ACTIVE_AUCTIONS_KEY = "global:active_auctions";

    @Override
    public Mono<Boolean> saveConfig(String configKey, QueueConfigModel model) {
        Map<String, String> configMap = model.toMap();

        return redisOps.opsForHash()
                .putAll(configKey, configMap);
    }

    @Override
    public Mono<QueueConfigModel> getConfig(String configKey) {
        return redisOps.opsForHash()
                .entries(configKey)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .filter(map -> !map.isEmpty())
                .map(map -> objectMapper.convertValue(map, QueueConfigModel.class));
    }

    @Override
    public Mono<Boolean> setExpiration(String key, Instant expireAt) {
        return redisOps.expireAt(key, expireAt);
    }

    @Override
    public Mono<Boolean> deleteConfig(String configKey) {
        return redisOps.opsForHash()
                .delete(configKey);
    }

    public Mono<Long> registerActiveAuction(UUID auctionId) {
        return redisOps.opsForSet()
                .add(GLOBAL_ACTIVE_AUCTIONS_KEY, auctionId);
    }

    public Flux<UUID> getAllActiveAuctions() {
        return redisOps.opsForSet()
                .members(GLOBAL_ACTIVE_AUCTIONS_KEY)
                .cast(UUID.class);
    }

    public Mono<Long> removeActiveAuction(UUID auctionId) {
        return redisOps.opsForSet()
                .remove(GLOBAL_ACTIVE_AUCTIONS_KEY, auctionId);
    }

    @Override
    public Mono<Boolean> addActiveUser(String activeKey, Long maxActive, UUID userId) {
        long now = System.currentTimeMillis();
        return redisOps.opsForZSet().size(activeKey)
                .flatMap(currentSize -> {
                    if (currentSize < maxActive)
                        return redisOps.opsForZSet().add(activeKey, userId, now);

                    return Mono.just(false);
                });
    }

    public Mono<Long> addAllActiveUser(String activeKey, List<UUID> userIds) {
        if (userIds.isEmpty())
            return Mono.just(0L);

        long now = System.currentTimeMillis();

        Set<ZSetOperations.TypedTuple<Object>> tuples = userIds.stream()
                .map(id -> ZSetOperations.TypedTuple.of((Object) id.toString(), (double) now))
                .collect(Collectors.toSet());

        return redisOps.opsForZSet()
                .addAll(activeKey, tuples);
    }

    public Mono<Long> activeUserCount(String activeKey) {
        return redisOps.opsForZSet().size(activeKey);
    }

    @Override
    public Mono<Boolean> addWaitingUser(String waitingKey, UUID userId) {
        long now = System.currentTimeMillis();
        // TODO waiting queue 용량 제한
        return redisOps.opsForZSet().add(waitingKey, userId, now);
    }

    public Mono<List<UUID>> popUserIdWaitingQueue(String waitingKey, long limit) {
        return redisOps.opsForZSet()
                .popMin(waitingKey, limit)
                .map(ZSetOperations.TypedTuple::getValue)
                .map(uuid -> UUID.fromString((String) uuid))
                .collectList();
    }

    @Override
    public Mono<Long> getRank(String waitingKey, UUID userId) {
        return redisOps.opsForZSet().rank(waitingKey, userId);
    }

    @Override
    public Mono<Boolean> saveToken(String tokenKey, Map<UUID, String> tokens) {
        return redisOps.opsForHash()
                .putAll(tokenKey, tokens);
    }
}
