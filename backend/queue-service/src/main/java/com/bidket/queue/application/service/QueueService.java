package com.bidket.queue.application.service;

import com.bidket.queue.infrastructure.redis.RedisUtils;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueService {
    private final ReactiveRedisOperations<String, Object> redisOps;

    public boolean createQueue(QueueCreateRequest request) {

        return false;
    }
}
