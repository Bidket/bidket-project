package com.bidket.queue.domain.repository;

import com.bidket.queue.domain.model.QueueConfigModel;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface RedisRepository {
    Mono<Boolean> saveConfig(String configKey, QueueConfigModel model);

    Mono<QueueConfigModel> getConfig(String configKey);

    Mono<Boolean> setConfigExpiration(String configKey, Instant expireAt);

    Mono<Boolean> deleteConfig(String configKey);

    Mono<Boolean> addActiveUser(String activeKey, Long maxUser, UUID userId);

    Mono<Boolean> addWaitingUser(String waitingKey, UUID userId);

    Mono<Long> getRank(String waitingKey, UUID userId);
}
