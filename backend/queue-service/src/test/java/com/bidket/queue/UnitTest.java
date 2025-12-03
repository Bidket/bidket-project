package com.bidket.queue;

import com.bidket.queue.application.service.QueueService;
import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.model.QueueConfigModel;
import com.bidket.queue.domain.model.QueueErrorCode;
import com.bidket.queue.infrastructure.redis.RedisRepositoryImpl;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UnitTest {
    @InjectMocks
    private QueueService queueService;

    @Mock
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
                .openAt(Instant.now())
                .closeAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        when(redisRepository.saveConfig(any(String.class), any(QueueConfigModel.class)))
                .thenReturn(Mono.just(true));
        when(redisRepository.setExpiration(any(String.class), any(Instant.class)))
                .thenReturn(Mono.just(true));
        when(redisRepository.registerActiveAuction(any(UUID.class)))
                .thenReturn(Mono.just(1L));

        // when
        Mono<QueueCreateResponse> response = queueService.createConfigQueue(request);

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
                .openAt(Instant.now())
                .closeAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        when(redisRepository.saveConfig(any(String.class), any(QueueConfigModel.class)))
                .thenReturn(Mono.just(false));


        Mono<QueueCreateResponse> response = queueService.createConfigQueue(request);

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
                .openAt(Instant.now())
                .closeAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        when(redisRepository.saveConfig(any(String.class), any(QueueConfigModel.class)))
                .thenReturn(Mono.just(true));
        when(redisRepository.setExpiration(any(String.class), any(Instant.class)))
                .thenReturn(Mono.just(false));

        Mono<QueueCreateResponse> response = queueService.createConfigQueue(request);

        StepVerifier.create(response)
                .expectErrorMatches(throwable ->
                        throwable instanceof QueueException &&
                                ((QueueException) throwable).getErrorCode() == QueueErrorCode.REDIS_EXPIRE_SET_FAILED
                )
                .verify();
    }

    @Test
    @DisplayName("성공: 대기열 입장 성공")
    void enterQueue_Enter_Success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        String configKey = "configKey";
        QueueConfigModel queueConfig = QueueConfigModel.builder()
                .auctionId(auctionId)
                .openAt(Instant.now())
                .closeAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .permitsPerSec(5)
                .maxActive(1000L)
                .build();

        when(redisRepository.getConfig(any(String.class)))
                .thenReturn(Mono.just(queueConfig));
        when(redisRepository.addWaitingUser(any(String.class), any()))
                .thenReturn(Mono.just(true));
        when(redisRepository.getRank(any(String.class), any()))
                .thenReturn(Mono.just(100L));

        // when
        Mono<QueueEnterResponse> response = queueService.enterQueue(userId, auctionId);

        // then
        StepVerifier.create(response)
                .expectNextMatches(result ->
                        result.token() == null &&
                                result.rank() == 100L &&
                                result.userId() == userId
                )
                .verifyComplete();
    }
}
