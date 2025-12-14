package com.bidket.queue.application.service;

import com.bidket.common.presentation.error.BaseErrorCode;
import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.model.HeartbeatStatus;
import com.bidket.queue.domain.model.QueueErrorCode;
import com.bidket.queue.domain.model.QueueTrafficStatus;
import com.bidket.queue.domain.model.UserStatus;
import com.bidket.queue.domain.repository.QueueManagementRepository;
import com.bidket.queue.domain.repository.QueueTrafficRepository;
import com.bidket.queue.global.annotation.CheckQueueConfig;
import com.bidket.queue.global.util.jwt.TokenProvider;
import com.bidket.queue.presentation.dto.response.QueueAccommodatableResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import com.bidket.queue.presentation.dto.response.QueueHeartbeatResponse;
import com.bidket.queue.presentation.dto.response.QueueStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueTrafficService {
    private final QueueTrafficRepository trafficRepository;
    private final QueueManagementRepository managementRepository;
    private final TokenProvider tokenProvider;

    @Value("${heartbeat.frequency}")
    private Long heartbeatFrequency;

    @CheckQueueConfig
    public Mono<QueueEnterResponse> enterQueue(UUID userId, UUID auctionId) {
        String waitingKey = "queue:auction:" + auctionId + ":waiting";
        return trafficRepository.addWaitingUser(auctionId, userId)
                .flatMap(isAdded -> trafficRepository.getRank(auctionId, userId))
                .flatMap(rank ->
                    managementRepository.setExpiration(waitingKey, Instant.now().plus(1, ChronoUnit.HOURS))
                            .thenReturn(QueueEnterResponse.builder()
                                    .auctionId(auctionId)
                                    .userId(userId)
                                    .rank(rank)
                                    .message("대기 중")
                                    .build())
                )
                .onErrorMap(e -> new QueueException(QueueErrorCode.REDIS_CONNECTION_ERROR, e.getMessage()));
    }

    @CheckQueueConfig
    public Mono<QueueAccommodatableResponse> isAccommodatable(UUID userId, UUID auctionId) {

        return trafficRepository.getToken(userId, auctionId)
                .flatMap(token -> {
                    if (!tokenProvider.validateToken(token, userId, auctionId))
                        return Mono.error(new QueueException(QueueErrorCode.INVALID_TOKEN));

                    return Mono.just(QueueAccommodatableResponse.builder()
                            .auctionId(auctionId)
                            .userId(userId)
                            .status(UserStatus.ACTIVE)
                            .rank(0L)
                            .retryAfter(0)
                            .token(token)
                            .message("입장이 가능합니다. 입찰 페이지로 이동합니다.")
                            .build());
                })
                .switchIfEmpty(trafficRepository.getRank(auctionId, userId)
                        .map(rank -> QueueAccommodatableResponse.builder()
                                .auctionId(auctionId)
                                .userId(userId)
                                .status(UserStatus.WAITING)
                                .rank(rank)
                                .retryAfter(3)
                                .token(null)
                                .message("현재 대기 인원 " + rank + "명 남았습니다.")
                                .build())
                        .switchIfEmpty(Mono.error(() -> new QueueException(QueueErrorCode.WAITING_USER_NOT_FOUND)))
                );

    }

    @CheckQueueConfig
    public Mono<Void> cancelWaiting(UUID userId, UUID auctionId) {
        String waitingKey = "queue:auction:" + auctionId + ":waiting";

        return trafficRepository.removeWaitingUser(waitingKey, userId)
                .then();
    }

    @CheckQueueConfig
    public Mono<QueueStatusResponse> getQueueStatus(UUID auctionId) {

        return Mono.zip(trafficRepository.getActiveUserCount(auctionId).defaultIfEmpty(0L),
                        trafficRepository.getWaitingUserCount(auctionId).defaultIfEmpty(0L),
                        managementRepository.getConfig(auctionId))
                .map(tuple ->
                        QueueStatusResponse.builder()
                                .auctionId(auctionId)
                                .totalWaiting(tuple.getT2())
                                .currentActive(tuple.getT1())
                                .status(QueueTrafficStatus.checkStatus(tuple.getT1(), tuple.getT3().maxActive()))
                                .build()
                )
                .switchIfEmpty(Mono.just(
                        QueueStatusResponse.builder()
                                .auctionId(auctionId)
                                .totalWaiting(0L)
                                .currentActive(0L)
                                .status(QueueTrafficStatus.SMOOTH)
                                .build()
                ))
                .onErrorMap(e -> new QueueException(QueueErrorCode.REDIS_CONNECTION_ERROR));
    }

    @CheckQueueConfig
    public Mono<QueueHeartbeatResponse> heartbeat(UUID userId, UUID auctionId, String currentToken) {
        return trafficRepository.getToken(userId, auctionId)
                .flatMap(savedToken -> {
                    if (!savedToken.equals(currentToken))
                        return Mono.error(new QueueException(QueueErrorCode.INVALID_TOKEN));

                    if (!tokenProvider.validateToken(currentToken, userId, auctionId)) {
                        return trafficRepository.kickActiveUser(auctionId, userId)
                                .map(kicked -> QueueHeartbeatResponse.builder()
                                        .userId(userId)
                                        .status(HeartbeatStatus.OUT)
                                        .build()
                                );
                    }

                    String tokenKey = "queue:token:" + auctionId;

                    return managementRepository.setExpiration(tokenKey, Instant.now().plus(heartbeatFrequency + 1, ChronoUnit.MINUTES))
                            .map(isSaved ->
                                    QueueHeartbeatResponse.builder()
                                            .userId(userId)
                                            .enterTime(tokenProvider.getIssuedAt(savedToken))
                                            .status(HeartbeatStatus.ACTIVE)
                                            .build()
                            );
                })
                .switchIfEmpty(Mono.error(new QueueException(QueueErrorCode.TOKEN_NOT_FOUND)));
    }
}