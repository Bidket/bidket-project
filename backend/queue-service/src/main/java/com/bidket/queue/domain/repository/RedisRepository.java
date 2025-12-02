package com.bidket.queue.domain.repository;

import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import reactor.core.publisher.Mono;

public interface RedisRepository {
    Mono<Object> getValue(String key);

    Mono<Boolean> setValue(String key, Object value);

    Mono<Boolean> deleteValue(String key);

    Mono<QueueCreateResponse> createQueueConfig(QueueCreateRequest request);
}
