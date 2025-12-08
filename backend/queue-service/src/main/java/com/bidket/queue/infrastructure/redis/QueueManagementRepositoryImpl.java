package com.bidket.queue.infrastructure.redis;

import com.bidket.queue.domain.model.QueueConfigModel;
import com.bidket.queue.domain.repository.QueueManagementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class QueueManagementRepositoryImpl implements QueueManagementRepository {
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

    @Override
    public Mono<Long> registerActiveAuction(UUID auctionId) {
        return redisOps.opsForSet()
                .add(GLOBAL_ACTIVE_AUCTIONS_KEY, auctionId);
    }

    @Override
    public Flux<UUID> getAllActiveAuctions() {
        return redisOps.opsForSet()
                .members(GLOBAL_ACTIVE_AUCTIONS_KEY)
                .map(uuid -> UUID.fromString((String) uuid));
    }

    @Override
    public Mono<Long> removeActiveAuction(UUID auctionId) {
        return redisOps.opsForSet()
                .remove(GLOBAL_ACTIVE_AUCTIONS_KEY, auctionId);
    }
}
