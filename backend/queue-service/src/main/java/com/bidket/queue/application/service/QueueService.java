package com.bidket.queue.application.service;

import com.bidket.queue.infrastructure.redis.RedisRepository;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class QueueService {
    private final RedisRepository redisRepository;

    public Mono<QueueCreateResponse> createQueue(QueueCreateRequest request) {
        return redisRepository.createQueueConfig(request);
    }
}
