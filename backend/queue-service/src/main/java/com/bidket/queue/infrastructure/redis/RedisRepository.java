package com.bidket.queue.infrastructure.redis;

import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

public interface RedisRepository {
    Mono<Object> getValue(String key);
    Mono<Boolean> setValue(String key, Object value);
    Mono<Boolean> deleteValue(String key);
    Mono<Boolean> createQueueConfig(QueueCreateRequest request);
}
