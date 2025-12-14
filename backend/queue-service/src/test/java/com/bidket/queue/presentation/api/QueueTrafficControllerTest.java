package com.bidket.queue.presentation.api;

import com.bidket.queue.application.facade.QueueFacade;
import com.bidket.queue.domain.model.HeartbeatStatus;
import com.bidket.queue.domain.model.QueueTrafficStatus;
import com.bidket.queue.presentation.dto.response.QueueAccommodatableResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import com.bidket.queue.presentation.dto.response.QueueHeartbeatResponse;
import com.bidket.queue.presentation.dto.response.QueueStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.BDDMockito.given;

@WebFluxTest(QueueTrafficController.class)
public class QueueTrafficControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private QueueFacade queueFacade;

    private UUID userId;
    private UUID auctionId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        auctionId = UUID.randomUUID();
    }

    @Test
    @DisplayName("성공: 대기열 입장 성공")
    void enterQueue_Success() {
        QueueEnterResponse response = QueueEnterResponse.builder()
                .auctionId(auctionId)
                .userId(userId)
                .rank(0L)
                .build();

        given(queueFacade.enterQueue(userId, auctionId))
                .willReturn(Mono.just(response));

        webTestClient.post()
                .uri("/v1/queues/{auctionId}", auctionId)
                .header("X-Member-Id", userId.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").exists();
    }

    @Test
    @DisplayName("성공: 대기열 수용여부 확인")
    void isAccommodatable_Success() {
        QueueAccommodatableResponse response = QueueAccommodatableResponse.builder()
                .auctionId(auctionId)
                .userId(userId)
                .token("test token")
                .build();

        given(queueFacade.isAccommodatable(userId, auctionId))
                .willReturn(Mono.just(response));

        webTestClient.get()
                .uri("/v1/queues/{auctionId}/status", auctionId)
                .header("X-Member-Id", userId.toString())
                .exchange()
                .expectHeader().exists("X-ACTIVE-TOKEN")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").exists();
    }

    @Test
    @DisplayName("성공: 대기 취소")
    void cancelWaiting_Success() {
        given(queueFacade.cancelWaiting(userId, auctionId))
                .willReturn(Mono.empty());

        webTestClient.delete()
                .uri("/v1/queues/{auctionId}", auctionId)
                .header("X-Member-Id", userId.toString())
                .exchange()
                .expectStatus().isNoContent()
                .expectBody()
                .consumeWith(System.out::println);
    }

    @Test
    @DisplayName("성공: 대기열 상태 확인")
    void getQueueStatus_Success() {
        QueueStatusResponse response = QueueStatusResponse.builder()
                .status(QueueTrafficStatus.SMOOTH)
                .totalWaiting(0L)
                .currentActive(0L)
                .build();

        given(queueFacade.getQueueStatus(auctionId))
                .willReturn(Mono.just(response));

        webTestClient.get()
                .uri("/v1/queues/{auctionId}", auctionId)
                .header("X-Member-Id", userId.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(System.out::println);
    }

    @Test
    @DisplayName("성공: 활동 상태 유지")
    void heartbeat_Success() {
        String token = "token";

        QueueHeartbeatResponse response = QueueHeartbeatResponse.builder()
                .activeToken("active token")
                .status(HeartbeatStatus.ACTIVE)
                .userId(userId)
                .build();

        given(queueFacade.heartbeat(userId, auctionId, token))
                .willReturn(Mono.just(response));

        webTestClient.post()
                .uri("/v1/queues/{auctionId}/heartbeat", auctionId)
                .header("X-Member-Id", userId.toString())
                .header("X-ACTIVE-TOKEN", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(System.out::println);
    }
}
