package com.bidket.queue.application.service;

import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.model.QueueConfigModel;
import com.bidket.queue.domain.model.QueueErrorCode;
import com.bidket.queue.domain.repository.QueueManagementRepository;
import com.bidket.queue.domain.repository.QueueTrafficRepository;
import com.bidket.queue.global.util.jwt.TokenProvider;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class QueueManagementServiceTest {

    @InjectMocks
    private QueueManagementService managementService;

    @Mock
    private QueueManagementRepository managementRepository;
    @Mock
    private QueueTrafficRepository trafficRepository;
    @Mock
    private TokenProvider tokenProvider;

    private UUID userId;
    private UUID auctionId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        auctionId = UUID.randomUUID();
    }

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

        when(managementRepository.saveConfig(any(UUID.class), any(QueueConfigModel.class)))
                .thenReturn(Mono.just(true));
        when(managementRepository.setExpiration(any(String.class), any(Instant.class)))
                .thenReturn(Mono.just(true));
        when(managementRepository.registerActiveAuction(any(UUID.class)))
                .thenReturn(Mono.just(1L));

        // when
        Mono<QueueCreateResponse> response = managementService.createConfigQueue(request);

        // then
        StepVerifier.create(response)
                .assertNext(result -> {
                    assert result.auctionId().equals(request.auctionId());
                    assert result.maxActive() == 100L;
                })
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

        when(managementRepository.saveConfig(any(UUID.class), any(QueueConfigModel.class)))
                .thenReturn(Mono.just(false));

        Mono<QueueCreateResponse> response = managementService.createConfigQueue(request);

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

        when(managementRepository.saveConfig(request.auctionId(), any(QueueConfigModel.class)))
                .thenReturn(Mono.just(true));
        when(managementRepository.setExpiration(any(String.class), any(Instant.class)))
                .thenReturn(Mono.just(false));

        Mono<QueueCreateResponse> response = managementService.createConfigQueue(request);

        StepVerifier.create(response)
                .expectErrorMatches(throwable ->
                        throwable instanceof QueueException &&
                                ((QueueException) throwable).getErrorCode() == QueueErrorCode.REDIS_EXPIRE_SET_FAILED
                )
                .verify();
    }
}
