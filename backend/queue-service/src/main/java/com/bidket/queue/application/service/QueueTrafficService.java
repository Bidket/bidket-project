package com.bidket.queue.application.service;

import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.model.HeartbeatStatus;
import com.bidket.queue.domain.model.QueueErrorCode;
import com.bidket.queue.domain.model.QueueStatus;
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
import java.util.Map;
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

        return trafficRepository.addWaitingUser(waitingKey, userId)
                .flatMap(isAdded -> trafficRepository.getRank(waitingKey, userId))
                .map(rank -> QueueEnterResponse.builder()
                        .auctionId(auctionId)
                        .userId(userId)
                        .rank(rank)
                        .retryAfter(3)
                        .message("대기 중")
                        .build());
    }

    @CheckQueueConfig
    public Mono<QueueAccommodatableResponse> isAccommodatable(UUID userId, UUID auctionId) {
        String waitingKey = "queue:auction:" + auctionId + ":waiting";

        return trafficRepository.getToken(userId, auctionId)
                .flatMap(token -> {
                    if (!tokenProvider.validateToken(token, userId, auctionId))
                        return Mono.error(new QueueException(QueueErrorCode.INVALID_TOKEN));

                    return Mono.just(QueueAccommodatableResponse.builder()
                            .auctionId(auctionId)
                            .userId(userId)
                            .status(UserStatus.ACTIVE)
                            .rank(0L)
                            .retryAfter(3)
                            .token(token)
                            .message("입장이 가능합니다. 입찰 페이지로 이동합니다.")
                            .build());
                })
                .switchIfEmpty(trafficRepository.getRank(waitingKey, userId)
                        .map(rank -> QueueAccommodatableResponse.builder()
                                .auctionId(auctionId)
                                .userId(userId)
                                .status(UserStatus.WAITING)
                                .rank(rank)
                                .retryAfter(0)
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
                        trafficRepository.getWaitingUserCount(auctionId).defaultIfEmpty(0L))
                .map(tuple ->
                        QueueStatusResponse.builder()
                                .auctionId(auctionId)
                                .totalWaiting(tuple.getT2())
                                .currentActive(tuple.getT1())
                                .status(QueueStatus.checkStatus(tuple.getT1()))
                                .build()
                )
                .switchIfEmpty(Mono.just(
                        QueueStatusResponse.builder()
                                .auctionId(auctionId)
                                .totalWaiting(0L)
                                .currentActive(0L)
                                .status(QueueStatus.checkStatus(0L))
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

                    return managementRepository.setExpiration(tokenKey, Instant.now().plus(heartbeatFrequency, ChronoUnit.MINUTES))
                            .map(isSaved ->
                                    QueueHeartbeatResponse.builder()
                                            .userId(userId)
                                            .status(HeartbeatStatus.ACTIVE)
                                            .build()
                            );
                })
                .switchIfEmpty(Mono.error(new QueueException(QueueErrorCode.TOKEN_NOT_FOUND)));
    }
}