package com.bidket.queue.application.service;

import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.jwt.TokenProvider;
import com.bidket.queue.domain.model.QueueConfigModel;
import com.bidket.queue.domain.model.QueueErrorCode;
import com.bidket.queue.domain.repository.RedisRepository;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueService {
    private final RedisRepository redisRepository;
    private final TokenProvider tokenProvider;

    public Mono<QueueCreateResponse> createConfigQueue(QueueCreateRequest request) {
        String key = "queue:auction:" + request.auctionId() + ":config";
        QueueConfigModel queueConfig = request.toModel();
        return redisRepository.saveConfig(key, queueConfig)
                .flatMap(isSuccess -> {
                    if (!isSuccess)
                        return Mono.error(new QueueException(QueueErrorCode.REDIS_SAVE_FAILED));

                    return redisRepository.setConfigExpiration(key, request.closeAt().plus(1, ChronoUnit.DAYS));
                })
                .onErrorResume(e -> {
                    if(e instanceof QueueException)
                        return Mono.error(e);

                    return redisRepository.deleteConfig(key)
                            .then(Mono.error(new QueueException(QueueErrorCode.REDIS_EXPIRE_SET_FAILED)));
                })
                .map(isExpireSetSuccess -> queueConfig.toCreateResponse())
                .onErrorMap(e -> {
                    if (e instanceof QueueException)
                        return e;

                    return new QueueException(QueueErrorCode.REDIS_CONNECTION_ERROR);
                });
    }

    public Mono<QueueEnterResponse> enterQueue(UUID userId, UUID auctionId, ServerRequest request) {
        String configKey = "queue:auction:" + auctionId + ":config";
        String activeKey = "queue:auction:" + auctionId + ":active";
        String waitingKey = "queue:auction:" + auctionId + ":waiting";

        return redisRepository.getConfig(configKey)
                .switchIfEmpty(Mono.error(new QueueException(QueueErrorCode.CONFIG_NOT_FOUND)))
                .flatMap(config -> {
                    config.checkOpenStatus(Instant.now());
                    return redisRepository.addActiveUser(activeKey, config.getMaxActive(), userId)
                            .flatMap(isActive -> {
                                if(!isActive)
                                    return redisRepository.addWaitingUser(waitingKey, userId)
                                            .flatMap(isWaiting -> redisRepository.getRank(waitingKey, userId))
                                            .map(rank -> QueueEnterResponse.builder()
                                                    .auctionId(auctionId)
                                                    .userId(userId)
                                                    .rank(rank)
                                                    .retryAfter(3)
                                                    .message("대기 중")
                                                    .build());

                                return Mono.just(QueueEnterResponse.builder()
                                        .auctionId(auctionId)
                                        .userId(userId)
                                        .rank(0L)
                                        .retryAfter(0)
                                        .message("입장 성공")
                                        .build());
                            });
                });
    }
}
