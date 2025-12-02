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
import java.util.Map;
import java.util.UUID;

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
        String key = "queue:auction:" + request.auctionId() + ":config";
        Map<String, Object> configMap = queueConfig.toMap();

        return redisOps.opsForHash()
                .putAll(key, configMap)
                .flatMap(isSuccess -> {
                    if (!isSuccess) {
                        return Mono.error(new QueueException(QueueErrorCode.REDIS_SAVE_FAILED));
                    }
                    return redisOps.expireAt(key, request.closeAt().plusDays(1L).toInstant(ZoneOffset.UTC));
                })
                .filter(isExpireSuccess -> isExpireSuccess)
                .switchIfEmpty(Mono.error(new QueueException(QueueErrorCode.REDIS_EXPIRE_SET_FAILED)))
                .map(flag -> queueConfig.toCreateResponse())
                .onErrorMap(e -> {
                    if (e instanceof QueueException)
                        return e;

                    return new QueueException(QueueErrorCode.REDIS_CONNECTION_ERROR);
                });
    }

    @Override
    public Mono<QueueEnterResponse> enterQueue(UUID userId, UUID auctionId) {
        String configKey = "queue:auction:" + auctionId + ":config";
        String activeKey = "queue:auction:" + auctionId + ":active";
        String waitingKey = "queue:auction:" + auctionId + ":waiting";

        long now = System.currentTimeMillis();

        return redisOps.opsForValue().get(configKey)
                .switchIfEmpty(Mono.error(new QueueException(QueueErrorCode.CONFIG_NOT_FOUND)))
                .flatMap(configJson -> {
                    return Mono.fromCallable(() -> objectMapper.readValue((JsonParser) configJson, QueueConfigModel.class))
                            .subscribeOn(Schedulers.boundedElastic())
                            .onErrorMap(e -> new QueueException(QueueErrorCode.AUCTION_CLOSED));
                })
                .flatMap(config -> {
                    config.checkOpenStatus(Instant.now());

                    return redisOps.opsForZSet().size(activeKey)
                            .flatMap(currentSize -> {
                                if (currentSize < config.getMaxActive()) {
                                    return redisOps.opsForZSet().add(activeKey, userId, now)
                                            .map(isSuccess -> {
                                                return QueueEnterResponse.builder()
                                                        .auctionId(auctionId)
                                                        .userId(userId)
                                                        .rank(0L)
                                                        .retryAfter(0)
                                                        .message("입장 성공")
                                                        .build();
                                            });
                                } else {
                                    return redisOps.opsForZSet().add(waitingKey, userId, now)
                                            .flatMap(isSuccess -> redisOps.opsForZSet().rank(waitingKey, userId))
                                            .map(rank -> {
                                                return QueueEnterResponse.builder()
                                                        .auctionId(auctionId)
                                                        .userId(userId)
                                                        .rank(rank)
                                                        .retryAfter(3)
                                                        .message("대기 중")
                                                        .build();
                                            });

                                }
                            });
                });
    }
}
