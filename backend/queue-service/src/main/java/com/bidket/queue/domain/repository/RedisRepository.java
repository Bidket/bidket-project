package com.bidket.queue.domain.repository;

import com.bidket.queue.domain.model.QueueConfigModel;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface RedisRepository {
    Mono<Object> getValue(String key);

    Mono<Boolean> setValue(String key, Object value);

    Mono<Boolean> deleteValue(String key);

    Mono<Boolean> saveConfig(String configKey, QueueConfigModel model);
    Mono<Boolean> setConfigExpiration(String configKey, Instant expireAt);

    Mono<QueueEnterResponse> enterQueue(UUID userId, UUID auctionId);
}
