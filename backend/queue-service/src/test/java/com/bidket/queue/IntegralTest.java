package com.bidket.queue;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.queue.presentation.api.QueueController;
import com.bidket.queue.presentation.dto.request.QueueCreateRequest;
import com.bidket.queue.presentation.dto.response.QueueCreateResponse;
import com.bidket.queue.presentation.dto.response.QueueEnterResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
public class IntegralTest {

    private static final Logger log = LoggerFactory.getLogger(IntegralTest.class);
    @Autowired
    private QueueController queueController;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void queueConfig() throws JsonProcessingException {
        QueueCreateRequest request = QueueCreateRequest.builder()
                .auctionId(UUID.randomUUID())
                .maxActive(100L)
                .permitsPerSec(1)
                .openAt(Instant.now())
                .closeAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .build();

        ResponseEntity<ApiResponse<QueueCreateResponse>> response = queueController.createQueueConfig(request).block();

        String key = "auction:config:" + request.auctionId() + ":config";

        log.info("response: \n {}", mapper.writeValueAsString(response));
    }

    @Test
    void enterQueue() throws JsonProcessingException {
        // given
        UUID userId = UUID.randomUUID();
        UUID auctionId = UUID.fromString("46867f96-661d-4e7a-a613-1e57c3645704");

        ResponseEntity<ApiResponse<QueueEnterResponse>> response = queueController.enterQueue(auctionId).block();

        log.info("response: \n {}", mapper.writeValueAsString(response));
    }
}
