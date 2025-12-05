package com.bidket.queue.domain.repository;

import com.bidket.queue.domain.model.QueueConfigModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface QueueManagementRepository {
    Mono<Boolean> saveConfig(String configKey, QueueConfigModel model);

    Mono<QueueConfigModel> getConfig(String configKey);

    Mono<Boolean> setExpiration(String configKey, Instant expireAt);

    Mono<Boolean> deleteConfig(String configKey);

    Mono<Long> registerActiveAuction(UUID auctionId);

    Flux<UUID> getAllActiveAuctions();

    Mono<Long> removeActiveAuction(UUID auctionId);
}
