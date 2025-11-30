package com.bidket.queue.domain.repository;

import com.bidket.queue.domain.model.QueueConfigModel;
import com.bidket.queue.infrastructure.redis.RedisRepository;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {
    private final ReactiveRedisOperations<String, Object> redisOps;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Object> getValue(String key) {
        return redisOps.opsForValue()
                .get(key)
                .map(String::valueOf);
    }

    @Override
    public Mono<Boolean> setValue(String key, Object value) {
        return redisOps.opsForValue()
                .set(key, value);
    }

    @Override
    public Mono<Boolean> deleteValue(String key) {
        return redisOps.opsForValue()
                .delete(key);
    }

    @Override
    public Mono<QueueCreateResponse> createQueueConfig(QueueCreateRequest request) {
        QueueConfigModel queueConfig = request.toModel();
        String key = "auction:config:" + request.auctionId() + ":config";
        Map<String, Object> configMap = queueConfig.toMap();

        return redisOps.opsForHash()
                .putAll(key, configMap)
                .flatMap(isSuccess -> {
                    if(isSuccess){
                        return redisOps.expireAt(key, request.closeAt().plusDays(1L).toInstant(ZoneOffset.UTC));
                    }
                    return Mono.just(false);
                })
                .map(flag -> queueConfig.toCreateResponse());
    }
}
