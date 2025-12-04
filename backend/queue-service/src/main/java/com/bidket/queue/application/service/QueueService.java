package com.bidket.queue.application.service;

import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.jwt.TokenProvider;
import com.bidket.queue.domain.model.QueueConfigModel;
import com.bidket.queue.domain.model.QueueErrorCode;
import com.bidket.queue.domain.model.QueueStatus;
import com.bidket.queue.domain.repository.RedisRepository;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import com.bidket.queue.presentation.dto.response.QueueStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {
    private final RedisRepository redisRepository;

    public Mono<QueueCreateResponse> createConfigQueue(QueueCreateRequest request) {
        String key = "queue:auction:" + request.auctionId() + ":config";
        QueueConfigModel queueConfig = request.toModel();
        return redisRepository.saveConfig(key, queueConfig)
                .flatMap(isSuccess -> {
                    if (!isSuccess)
                        return Mono.error(new QueueException(QueueErrorCode.REDIS_SAVE_FAILED));

                    return redisRepository.setExpiration(key, request.closeAt().plus(1, ChronoUnit.DAYS));
                })
                .onErrorResume(e -> {
                    if(e instanceof QueueException)
                        return Mono.error(e);

                    return redisRepository.deleteConfig(key)
                            .then(Mono.error(new QueueException(QueueErrorCode.REDIS_EXPIRE_SET_FAILED)));
                })
                .flatMap(isExpireSuccess -> {
                    if (isExpireSuccess) {
                        return redisRepository.registerActiveAuction(request.auctionId())
                                .onErrorMap(e -> new QueueException(QueueErrorCode.AUCTION_REGISTER_FAIL))
                                .map(addedCount -> queueConfig.toCreateResponse());
                    }
                    return Mono.error(new QueueException(QueueErrorCode.REDIS_EXPIRE_SET_FAILED));
                })
                .onErrorMap(e -> {
                    if (e instanceof QueueException)
                        return e;

                    return new QueueException(QueueErrorCode.REDIS_CONNECTION_ERROR);
                });
    }

    public Mono<QueueEnterResponse> enterQueue(UUID userId, UUID auctionId) {
        String configKey = "queue:auction:" + auctionId + ":config";
        String waitingKey = "queue:auction:" + auctionId + ":waiting";

        return redisRepository.getConfig(configKey)
                .switchIfEmpty(Mono.error(new QueueException(QueueErrorCode.CONFIG_NOT_FOUND)))
                .flatMap(config -> {
                    config.checkOpenStatus(Instant.now());
                    log.info("사용자[{}]: 대기열 입장[{}]", userId, waitingKey);
                    return redisRepository.addWaitingUser(waitingKey, userId);
                })
                .flatMap(isAdded -> redisRepository.getRank(waitingKey, userId))
                .map(rank -> QueueEnterResponse.builder()
                            .auctionId(auctionId)
                            .userId(userId)
                            .rank(rank)
                            .retryAfter(3)
                            .message("대기 중")
                            .build());
    }

    public Mono<QueueStatusResponse> getQueueStatus(UUID userId, UUID auctionId) {
        String tokenKey = "queue:token:" + auctionId;
        String waitingKey = "queue:auction:" + auctionId + ":waiting";
        String configKey = "queue:auction:" + auctionId + ":config";

        return redisRepository.getConfig(configKey)
                .switchIfEmpty(Mono.error(new QueueException(QueueErrorCode.CONFIG_NOT_FOUND)))
                .flatMap(config -> {
                    config.checkOpenStatus(Instant.now());
                    log.info("사용자[{}]: 대기열 상태 확인[{}]", userId, waitingKey);
                    return redisRepository.getToken(tokenKey, userId);
                })
                .flatMap(token ->
                        Mono.just(QueueStatusResponse.builder()
                                .auctionId(auctionId)
                                .userId(userId)
                                .status(QueueStatus.ACTIVE)
                                .rank(0L)
                                .retryAfter(3)
                                .token(token)
                                .message("입장이 가능합니다. 입찰 페이지로 이동합니다.")
                                .build())
                )
                .switchIfEmpty(redisRepository.getRank(waitingKey, userId)
                        .map(rank -> QueueStatusResponse.builder()
                                .auctionId(auctionId)
                                .userId(userId)
                                .status(QueueStatus.WAITING)
                                .rank(rank)
                                .retryAfter(0)
                                .token(null)
                                .message("현재 대기 인원 " + rank + "명 남았습니다.")
                                .build())
                        .switchIfEmpty(Mono.error(() -> new QueueException(QueueErrorCode.WAITING_USER_NOT_FOUND)))
                );

    }
}