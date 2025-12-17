package com.bidket.queue.presentation.api;

import com.bidket.queue.application.facade.QueueFacade;
import com.bidket.queue.presentation.dto.request.QueueConfigUpdateRequest;
import com.bidket.queue.presentation.dto.response.QueueConfigUpdateResponse;
import com.bidket.queue.presentation.dto.response.QueueMetricsResponse;
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

@WebFluxTest(QueueAdminController.class)
public class QueueAdminControllerTest {

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
    @DisplayName("성공: 대기열 정책 변경")
    void updateConfig_Success() {
        QueueConfigUpdateRequest request = QueueConfigUpdateRequest.builder().build();
        QueueConfigUpdateResponse response = QueueConfigUpdateResponse.builder()
                .auctionId(auctionId)
                .build();

        given(queueFacade.updateConfig(userId, auctionId, request))
                .willReturn(Mono.just(response));

        webTestClient.patch()
                .uri("/v1/admin/queues/{auctionId}", auctionId)
                .body(Mono.just(request), QueueConfigUpdateRequest.class)
                .header("X-Member-Id", userId.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    @Test
    @DisplayName("성공: 대기열 현황 상세 모니터링")
    void getMetrics_Success() {
        QueueMetricsResponse response = QueueMetricsResponse.builder()
                .auctionId(auctionId)
                .build();

        given(queueFacade.getMetrics(auctionId))
                .willReturn(Mono.just(response));

        webTestClient.get()
                .uri("/v1/admin/queues/{auctionId}/metrics", auctionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }
}
