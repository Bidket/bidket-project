package com.bidket.queue.presentation.api;

import com.bidket.queue.application.facade.QueueFacade;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.BDDMockito.given;

@WebFluxTest(QueueInternalController.class)
public class QueueInternalControllerTest {

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
    @DisplayName("성공: 큐 설정 생성")
    void createQueueConfig_Success() {
        QueueCreateRequest request = QueueCreateRequest.builder()
                .auctionId(auctionId)
                .closeAt(Instant.now())
                .openAt(Instant.now())
                .maxActive(300L)
                .permitsPerSec(10)
                .build();

        QueueCreateResponse response = QueueCreateResponse.builder()
                .auctionId(auctionId)
                .build();

        given(queueFacade.createConfigQueue(request))
                .willReturn(Mono.just(response));

        webTestClient.post()
                .uri("/v1/internal/queues")
                .body(Mono.just(request), QueueCreateRequest.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .consumeWith(System.out::println);
    }

    @Test
    @DisplayName("성공: 큐 폐쇄")
    void closeQueue_Success() {
        given(queueFacade.closeQueue(auctionId))
                .willReturn(Mono.empty());

        webTestClient.delete()
                .uri("/v1/internal/queues/{auctionId}", auctionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(System.out::println);
    }
}
