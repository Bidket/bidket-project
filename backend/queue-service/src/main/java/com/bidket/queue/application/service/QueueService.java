package com.bidket.queue.application.service;

import com.bidket.queue.domain.jwt.TokenProvider;
import com.bidket.queue.domain.repository.RedisRepository;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueService {
    private final RedisRepository redisRepository;
    private final TokenProvider tokenProvider;

    public Mono<QueueCreateResponse> createConfigQueue(QueueCreateRequest request) {
        return redisRepository.createQueueConfig(request);
    }

    public Mono<QueueEnterResponse> queueEntrance(UUID userId, UUID auctionId) {
        tokenProvider.generateToken(userId, auctionId);
        return null;
    }
}
