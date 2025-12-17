package com.bidket.queue.application.service;

import com.bidket.queue.domain.model.HeartbeatStatus;
import com.bidket.queue.domain.model.QueueConfigModel;
import com.bidket.queue.domain.model.QueueConfigStatus;
import com.bidket.queue.domain.model.UserStatus;
import com.bidket.queue.domain.repository.QueueManagementRepository;
import com.bidket.queue.domain.repository.QueueTrafficRepository;
import com.bidket.queue.global.util.jwt.TokenProvider;
import com.bidket.queue.presentation.dto.response.QueueAccommodatableResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import com.bidket.queue.presentation.dto.response.QueueHeartbeatResponse;
import com.bidket.queue.presentation.dto.response.QueueStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QueueTrafficServiceTest {

    @InjectMocks
    private QueueTrafficService trafficService;

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
    @DisplayName("성공: 대기열 입장 성공")
    void enterQueue_Enter_Success() {
        // given
        when(trafficRepository.addWaitingUser(auctionId, userId))
                .thenReturn(Mono.just(true));
        when(trafficRepository.getRank(auctionId, userId))
                .thenReturn(Mono.just(1L));
        when(managementRepository.setExpiration(any(String.class), any(Instant.class)))
                .thenReturn(Mono.just(true));

        // when
        Mono<QueueEnterResponse> response = trafficService.enterQueue(userId, auctionId);

        // then
        StepVerifier.create(response)
                .assertNext(result -> {
                    assert result.token() == null;
                    assert result.rank() == 1L;
                    assert result.userId() == userId;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("성공: 대기열 수용 가능")
    void isAccommodatable_Possible_Success() {
        //given
        String token = "token";

        when(trafficRepository.getToken(userId, auctionId))
                .thenReturn(Mono.just(token));
        when(tokenProvider.validateToken(token, userId, auctionId))
                .thenReturn(true);
        when(trafficRepository.getRank(auctionId, userId))
                .thenReturn(Mono.just(0L));

        // when
        Mono<QueueAccommodatableResponse> response = trafficService.isAccommodatable(userId, auctionId);

        StepVerifier.create(response)
                .assertNext(result -> {
                    assert result.token().equals(token);
                    assert result.auctionId().equals(auctionId);
                    assert result.status().equals(UserStatus.ACTIVE);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("성공: 대기열 수용 불가")
    void isAccommodatable_Impossible_Success() {
        //given
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();

        when(trafficRepository.getToken(userId, auctionId))
                .thenReturn(Mono.empty());
        when(trafficRepository.getRank(auctionId, userId))
                .thenReturn(Mono.just(100L));

        // when
        Mono<QueueAccommodatableResponse> response = trafficService.isAccommodatable(userId, auctionId);

        StepVerifier.create(response)
                .assertNext(result -> {
                    assert result.token() == null;
                    assert result.auctionId().equals(auctionId);
                    assert result.status().equals(UserStatus.WAITING);
                    assert result.rank() == 100L;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("성공: 대기 취소")
    void cancelWaiting_Success() {
        // given
        String waitingKey = "queue:auction:" + auctionId + ":waiting";

        when(trafficRepository.removeWaitingUser(waitingKey, userId))
                .thenReturn(Mono.just(1L));

        // when
        Mono<Void> response = trafficService.cancelWaiting(userId, auctionId);

        StepVerifier.create(response)
                .verifyComplete();

        verify(trafficRepository).removeWaitingUser(waitingKey, userId);
    }

    @Test
    @DisplayName("성공: 대기열 상태 조회")
    void getQueueStatus_Success() {
        // given
        long currentWaitingUser = 300L;
        long currentActiveUser = 10L;

        QueueConfigModel config = QueueConfigModel.builder()
                .auctionId(auctionId)
                .maxActive(currentActiveUser)
                .permitsPerSec(10)
                .status(QueueConfigStatus.RUNNING)
                .build();

        when(trafficRepository.getActiveUserCount(auctionId))
                .thenReturn(Mono.just(currentActiveUser));
        when(trafficRepository.getWaitingUserCount(auctionId))
                .thenReturn(Mono.just(currentWaitingUser));
        when(managementRepository.getConfig(auctionId))
                .thenReturn(Mono.just(config));

        // when
        Mono<QueueStatusResponse> response = trafficService.getQueueStatus(auctionId);

        // then
        StepVerifier.create(response)
                .assertNext(result -> {
                    assert result.auctionId().equals(auctionId);
                    assert result.totalWaiting().equals(currentWaitingUser);
                    assert result.currentActive().equals(currentActiveUser);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("성공: 대기열 상태 조회 - Active 대기열 존재하지 않음")
    void getQueueStatus_Active_NotExist_Success() {
        // given
        long currentWaitingUser = 0L;
        long currentActiveUser = 0L;

        when(trafficRepository.getActiveUserCount(auctionId))
                .thenReturn(Mono.empty());
        when(trafficRepository.getWaitingUserCount(auctionId))
                .thenReturn(Mono.empty());
        when(managementRepository.getConfig(auctionId))
                .thenReturn(Mono.empty());

        // when
        Mono<QueueStatusResponse> response = trafficService.getQueueStatus(auctionId);

        // then
        StepVerifier.create(response)
                .assertNext(result -> {
                    assert result.auctionId().equals(auctionId);
                    assert result.totalWaiting().equals(currentWaitingUser);
                    assert result.currentActive().equals(currentActiveUser);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("성공: 사용자 활동 유지 확인")
    void heartbeat_Success() {
        // given
        String token = "token";

        ReflectionTestUtils.setField(trafficService, "heartbeatFrequency", 3L);

        when(trafficRepository.getToken(userId, auctionId))
                .thenReturn(Mono.just(token));
        when(tokenProvider.validateToken(token, userId, auctionId))
                .thenReturn(true);
        when(managementRepository.setExpiration(any(String.class), any(Instant.class)))
                .thenReturn(Mono.just(true));

        // when
        Mono<QueueHeartbeatResponse> response = trafficService.heartbeat(userId, auctionId, token);

        StepVerifier.create(response)
                .assertNext(result -> {
                    assert result.userId().equals(userId);
                    assert result.status().equals(HeartbeatStatus.ACTIVE);
                })
                .verifyComplete();
    }
}
