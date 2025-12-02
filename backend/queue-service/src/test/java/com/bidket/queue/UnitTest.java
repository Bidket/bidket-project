package com.bidket.queue;

import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.model.QueueErrorCode;
import com.bidket.queue.infrastructure.redis.RedisRepositoryImpl;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UnitTest {
    @InjectMocks
    private RedisRepositoryImpl redisRepository;

    @Mock
    private ReactiveRedisOperations<String, Object> redisOps;

    @Mock
    private ReactiveHashOperations<String, String, Object> hashOps;

    @Test
    @DisplayName("성공: config queue 생성")
    void createQueueConfig_Success() {
        // given
        QueueCreateRequest request = QueueCreateRequest.builder()
                .auctionId(UUID.randomUUID())
                .maxActive(100L)
                .permitsPerSec(1)
                .openAt(LocalDateTime.now())
                .closeAt(LocalDateTime.now().plusHours(5L))
                .build();

        doReturn(hashOps).when(redisOps).opsForHash();

        when(hashOps.putAll(any(String.class), any()))
                .thenReturn(Mono.just(true));
        when(redisOps.expireAt(any(String.class), any(Instant.class)))
                .thenReturn(Mono.just(true));

        // when
        Mono<QueueCreateResponse> response = redisRepository.createQueueConfig(request);

        StepVerifier.create(response)
                .expectNextMatches(result ->
                        result.auctionId().equals(request.auctionId()) &&
                                result.maxActive() == 100L
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("실패: Redis 저장 실패")
    void createQueueConfig_Fail_Save() {
        // given
        QueueCreateRequest request = QueueCreateRequest.builder()
                .auctionId(UUID.randomUUID())
                .maxActive(100L)
                .permitsPerSec(1)
                .openAt(LocalDateTime.now())
                .closeAt(LocalDateTime.now().plusHours(5L))
                .build();

        doReturn(hashOps).when(redisOps).opsForHash();

        when(hashOps.putAll(any(String.class), any()))
                .thenReturn(Mono.just(false));

        Mono<QueueCreateResponse> response = redisRepository.createQueueConfig(request);

        StepVerifier.create(response)
                .expectErrorMatches(throwable ->
                        throwable instanceof QueueException &&
                        ((QueueException) throwable).getErrorCode() == QueueErrorCode.REDIS_SAVE_FAILED
                )
                .verify();
    }

    @Test
    @DisplayName("실패: Redis 만료 설정 실패")
    void createQueueConfig_Expire_Fail() {
        // given
        QueueCreateRequest request = QueueCreateRequest.builder()
                .auctionId(UUID.randomUUID())
                .maxActive(100L)
                .permitsPerSec(1)
                .openAt(LocalDateTime.now())
                .closeAt(LocalDateTime.now().plusHours(5L))
                .build();

        doReturn(hashOps).when(redisOps).opsForHash();

        when(hashOps.putAll(any(String.class), any()))
                .thenReturn(Mono.just(true));
        when(redisOps.expireAt(any(String.class), any()))
                .thenReturn(Mono.just(false));

        Mono<QueueCreateResponse> response = redisRepository.createQueueConfig(request);

        StepVerifier.create(response)
                .expectErrorMatches(throwable ->
                        throwable instanceof QueueException &&
                                ((QueueException) throwable).getErrorCode() == QueueErrorCode.REDIS_EXPIRE_SET_FAILED
                )
                .verify();
    }
}
