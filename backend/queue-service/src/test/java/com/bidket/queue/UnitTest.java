package com.bidket.queue;

import com.bidket.queue.application.facade.QueueFacade;
import com.bidket.queue.application.service.QueueManagementService;
import com.bidket.queue.application.service.QueueTrafficService;
import com.bidket.queue.domain.exception.QueueException;
import com.bidket.queue.domain.model.QueueConfigModel;
import com.bidket.queue.domain.model.QueueErrorCode;
import com.bidket.queue.domain.repository.QueueManagementRepository;
import com.bidket.queue.domain.repository.QueueTrafficRepository;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UnitTest {

    @InjectMocks
    private QueueTrafficService trafficService;
    @InjectMocks
    private QueueManagementService managementService;

    @Mock
    private QueueManagementRepository managementRepository;
    @Mock
    private QueueTrafficRepository trafficRepository;

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

        when(trafficRepository.addWaitingUser(auctionId, userId))
                .thenReturn(Mono.just(true));
        when(trafficRepository.getRank(auctionId, userId))
                .thenReturn(Mono.just(1L));

        // when
        Mono<QueueEnterResponse> response = trafficService.enterQueue(userId, auctionId);

        // then
        StepVerifier.create(response)
                .expectNextMatches(result ->
                        result.token() == null &&
                                result.rank() == 1L &&
                                result.userId() == userId
                )
                .verifyComplete();
    }
}
