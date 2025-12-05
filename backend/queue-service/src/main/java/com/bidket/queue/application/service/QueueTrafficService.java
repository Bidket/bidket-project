package com.bidket.queue.application.service;

import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.model.QueueErrorCode;
import com.bidket.queue.domain.model.QueueStatus;
import com.bidket.queue.domain.repository.QueueTrafficRepository;
import com.bidket.queue.global.annotation.CheckQueueConfig;
import com.bidket.queue.global.util.jwt.TokenProvider;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import com.bidket.queue.presentation.dto.response.QueueStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueTrafficService {
    private final QueueTrafficRepository trafficRepository;
    private final TokenProvider tokenProvider;

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
    public Mono<QueueStatusResponse> getQueueStatus(UUID userId, UUID auctionId) {
        String tokenKey = "queue:token:" + auctionId;
        String waitingKey = "queue:auction:" + auctionId + ":waiting";

        return trafficRepository.getToken(tokenKey, userId)
                .flatMap(token -> {
                    if (!tokenProvider.validateToken(token, userId, auctionId))
                        return Mono.error(new QueueException(QueueErrorCode.INVALID_TOKEN));

                    return Mono.just(QueueStatusResponse.builder()
                            .auctionId(auctionId)
                            .userId(userId)
                            .status(QueueStatus.ACTIVE)
                            .rank(0L)
                            .retryAfter(3)
                            .token(token)
                            .message("입장이 가능합니다. 입찰 페이지로 이동합니다.")
                            .build());
                })
                .switchIfEmpty(trafficRepository.getRank(waitingKey, userId)
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

    @CheckQueueConfig
    public Mono<Void> cancelWaiting(UUID userId, UUID auctionId) {
        String waitingKey = "queue:auction:" + auctionId + ":waiting";

        return trafficRepository.removeWaitingUser(waitingKey, userId)
                .then();
    }
}