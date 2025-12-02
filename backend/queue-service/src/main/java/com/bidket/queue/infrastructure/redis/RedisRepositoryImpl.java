package com.bidket.queue.infrastructure.redis;

import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.model.QueueConfigModel;
import com.bidket.queue.domain.model.QueueErrorCode;
import com.bidket.queue.domain.repository.RedisRepository;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {
    private final ReactiveRedisOperations<String, Object> redisOps;
    private final ObjectMapper objectMapper;

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
    public Mono<Boolean> setConfigExpiration(String key, Instant expireAt) {
        return redisOps.expireAt(key, expireAt);
    }

    @Override
    public Mono<Boolean> deleteConfig(String configKey) {
        return redisOps.opsForHash()
                .delete(configKey);
    }

    @Override
    public Mono<Boolean> addActiveUser(String activeKey, Long maxActive, UUID userId) {
        long now = System.currentTimeMillis();
        return redisOps.opsForZSet().size(activeKey)
                .flatMap(currentSize -> {
                    if(currentSize < maxActive)
                        return redisOps.opsForZSet().add(activeKey, userId, now);

                    return Mono.just(false);
                });
    }

    @Override
    public Mono<Boolean> addWaitingUser(String waitingKey, UUID userId) {
        long now = System.currentTimeMillis();
        return redisOps.opsForZSet().add(waitingKey, userId, now);
    }

    @Override
    public Mono<Long> getRank(String waitingKey, UUID userId) {
        return redisOps.opsForZSet().rank(waitingKey, userId);
    }
}
